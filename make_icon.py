"""Generates the mod icon (src/main/resources/autoclicker.png) - a dark panel with a
cursor arrow and click waves. Run:  python3 make_icon.py"""
from PIL import Image, ImageDraw

S = 128
BG = (16, 19, 28, 255)
PANEL = (24, 28, 40, 255)
BORDER = (59, 66, 82, 255)
ACCENT = (85, 255, 85, 255)
CURSOR = (216, 222, 233, 255)
SHADOW = (10, 12, 18, 255)

img = Image.new("RGBA", (S, S), BG)
d = ImageDraw.Draw(img)

# panel
d.rounded_rectangle([8, 8, S - 9, S - 9], radius=12, fill=PANEL, outline=BORDER, width=2)

# click waves
for i, r in enumerate((26, 38, 50)):
    if i == 0:
        w = 4
    else:
        w = 3
    d.arc([S // 2 - r, S // 2 - r + 6, S // 2 + r, S // 2 + r + 6], start=-115, end=-15, fill=ACCENT, width=w)

# cursor arrow (classic pointer, drawn as a polygon then scaled)
arrow = [(0, 0), (0, 46), (12, 34), (20, 52), (28, 48), (20, 30), (34, 30)]
pts = [(56 + x, 44 + y) for (x, y) in arrow]
d.polygon([(x + 2, y + 2) for (x, y) in pts], fill=SHADOW)
d.polygon(pts, fill=CURSOR, outline=(140, 150, 170, 255))

img.save("src/main/resources/autoclicker.png")
print("wrote src/main/resources/autoclicker.png", img.size)
