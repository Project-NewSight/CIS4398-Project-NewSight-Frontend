import colorsys

def hsl_to_hex(h, s, l):
    # CSS HSL is H(0-360), S(0-100%), L(0-100%)
    # Python hls_to_rgb takes h(0-1), l(0-1), s(0-1)
    r, g, b = colorsys.hls_to_rgb(h/360.0, l/100.0, s/100.0)
    return '#{:02x}{:02x}{:02x}'.format(int(r*255), int(g*255), int(b*255))

colors = {
    "background": (220, 15, 8),
    "foreground": (220, 10, 95),
    "card": (220, 15, 12),
    "surface_1": (220, 15, 14),
    "surface_2": (220, 15, 18),
    "primary": (215, 90, 65), # Dark mode primary
    "secondary": (220, 15, 20),
    "accent": (170, 80, 50),
    "muted": (220, 15, 16),
    "border": (220, 15, 20)
}

for name, (h, s, l) in colors.items():
    print(f'{name}: {hsl_to_hex(h, s, l)}')
