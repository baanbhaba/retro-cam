import sys
import time
from playwright.sync_api import sync_playwright

def run_tests():
    print("=== Starting RetroCam Camera Selector & UI Test Suite ===")
    errors = []
    
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 600, "height": 950})
        page = context.new_page()

        console_logs = []
        page.on("console", lambda msg: console_logs.append(f"[{msg.type}] {msg.text}"))
        page.on("pageerror", lambda err: errors.append(f"Page Error: {err}"))

        # 1. Load the page
        print("[TEST 1] Loading http://localhost:8765 ...")
        res = page.goto("http://localhost:8765", wait_until="networkidle")
        assert res.status == 200, f"Failed to load, status: {res.status}"
        print("  ✓ Page loaded with status 200")

        # 2. Check layout - verify old carousel is removed from main screen!
        print("[TEST 2] Verifying clean spacious layout (no middle carousel) ...")
        assert page.is_visible("#phone-chassis"), "Phone chassis missing"
        assert page.is_visible("#top-bar"), "Top bar missing"
        assert page.is_visible("#viewfinder-container"), "Viewfinder missing"
        assert not page.locator("#preset-carousel").is_visible(), "Old preset carousel should be removed from main screen!"
        assert page.is_visible("#controls-container"), "Controls panel missing"
        assert page.is_visible("#btn-open-camera-selector"), "Dedicated Camera Selector button missing in controls!"
        print("  ✓ Old crowded carousel removed; dedicated camera button present")

        # 3. Test Dedicated Camera Selector Page
        print("[TEST 3] Testing Dedicated Camera Selector Page Modal ...")
        cam_sheet = page.locator("#camera-selector-sheet")
        assert not cam_sheet.is_visible(), "Camera selector sheet should start hidden"

        # Click the dedicated camera button
        page.locator("#btn-open-camera-selector").click()
        page.wait_for_timeout(200)
        assert cam_sheet.is_visible(), "Camera selector sheet failed to open!"
        print("  ✓ Camera selector catalog opened smoothly")

        # Check all 6 vintage camcorder models are present in catalog
        models = [
            "SONY CCD-TRV65",
            "PANASONIC NV-GS400",
            "JVC GR-AXM10",
            "SONY DCR-VX1000",
            "HANDYCAM NIGHTSHOT",
            "SECURITY CCD MONITOR"
        ]
        for m in models:
            assert page.locator(f".camera-model-card:has-text('{m}')").is_visible(), f"Model {m} missing from catalog!"
        print("  ✓ All 6 vintage camcorder models listed with specs")

        # Select PANASONIC NV-GS400 (MINI_DV)
        page.locator(".camera-model-card[data-preset='MINI_DV']").click()
        page.wait_for_timeout(300)
        assert not cam_sheet.is_visible(), "Camera selector sheet did not close after equipping!"
        
        # Verify button label and HUD tag updated
        btn_label = page.locator("#btn-open-camera-selector").inner_text()
        hud_tag = page.locator("#hud-preset-tag").inner_text()
        print(f"  ✓ Camera equipped. Button label: {btn_label}, HUD tag: {hud_tag}")
        assert "MINI-DV" in btn_label, f"Expected MINI-DV on button, got {btn_label}"
        assert "MINI-DV" in hud_tag, f"Expected MINI-DV on HUD, got {hud_tag}"

        # Test opening via clicking HUD tag
        page.locator("#hud-preset-tag").click()
        page.wait_for_timeout(200)
        assert cam_sheet.is_visible(), "Failed to open camera catalog via clicking HUD tag!"
        print("  ✓ Opened camera catalog by clicking HUD tag")

        # Select SONY DCR-VX1000 (SKATE_DV)
        page.locator(".camera-model-card[data-preset='SKATE_DV']").click()
        page.wait_for_timeout(300)
        btn_label_skate = page.locator("#btn-open-camera-selector").inner_text()
        print(f"  ✓ Switched to Skate DV: {btn_label_skate}")
        assert "SKATE DV" in btn_label_skate, f"Expected SKATE DV, got {btn_label_skate}"

        # 4. Test Tap-to-Focus
        print("[TEST 4] Testing Tap-to-Focus interaction ...")
        focus_container = page.locator("#focus-container")
        vf = page.locator("#viewfinder-container")
        vf_box = vf.bounding_box()
        page.mouse.click(vf_box["x"] + 150, vf_box["y"] + 160)
        page.wait_for_timeout(200)
        assert focus_container.is_visible(), "Focus container did not show after tap!"
        print("  ✓ Focus reticle appeared successfully")

        # 5. Test Exposure Slider
        print("[TEST 5] Testing Exposure Slider Drag ...")
        rail = page.locator("#focus-exposure-rail")
        rail_box = rail.bounding_box()
        page.mouse.move(rail_box["x"] + rail_box["width"] / 2, rail_box["y"] + rail_box["height"] / 2)
        page.mouse.down()
        page.mouse.move(rail_box["x"] + rail_box["width"] / 2, rail_box["y"] + rail_box["height"] / 2 - 40, steps=5)
        page.mouse.up()
        page.wait_for_timeout(200)
        ev_after = page.locator("#slider-ev-val").inner_text()
        print(f"  ✓ EV after drag: {ev_after}")
        assert "+" in ev_after, "EV should be positive after dragging up"

        # 6. Test Zoom Rocker
        print("[TEST 6] Testing Zoom Rocker ...")
        page.locator("#btn-zoom-t").click()
        page.wait_for_timeout(100)
        zoom_val = page.locator("#zoom-indicator-text").inner_text()
        print(f"  ✓ Zoom value: {zoom_val}")
        assert "1.5x" in zoom_val, f"Expected 1.5x, got {zoom_val}"

        # 7. Test Video Recording
        print("[TEST 7] Testing Video Recording Shutter ...")
        page.locator("#btn-mode-video").click()
        page.wait_for_timeout(100)
        page.locator("#main-shutter").click()
        page.wait_for_timeout(1100)
        assert page.locator("#hud-status-text").inner_text() == "REC", "Not recording!"
        assert page.locator("#rec-pulse-border").is_visible(), "Recording border not visible!"
        page.locator("#main-shutter").click()
        page.wait_for_timeout(100)
        assert page.locator("#hud-status-text").inner_text() == "STBY", "Not stopped!"
        print("  ✓ Video recording start/stop verified")

        page.screenshot(path="test_clean_camera_selector.png")
        print("  ✓ Saved test_clean_camera_selector.png")

        # 8. Check console errors
        critical_errors = [e for e in errors if "favicon" not in e.lower()]
        assert not critical_errors, f"JavaScript Errors: {critical_errors}"
        print("  ✓ Zero JavaScript / Page errors!")

        browser.close()
        print("\n🎉 ALL TESTS PASSED! Dedicated Camera Selector verified! 🎉")

if __name__ == "__main__":
    run_tests()
