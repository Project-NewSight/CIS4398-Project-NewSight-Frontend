import colorsys

def hsl_to_hex(h, s, l):
    r, g, b = colorsys.hls_to_rgb(h/360.0, l/100.0, s/100.0)
    return '#{:02x}{:02x}{:02x}'.format(int(r*255), int(g*255), int(b*255))

# Light mode colors from React
light_colors = {
    "background": (210, 20, 98),
    "foreground": (220, 15, 10),
    "card": (0, 0, 100),
    "surface_1": (210, 20, 96),
    "surface_2": (210, 20, 92),
    "primary": (215, 90, 45),
    "secondary": (215, 15, 85),
    "muted": (210, 10, 94),
    "muted_foreground": (215, 10, 40),
    "accent": (170, 80, 40),
    "border": (220, 10, 90),
    "input_bg": (220, 10, 90),
}

print("Light Mode Colors:")
for name, (h, s, l) in light_colors.items():
    print(f'{name}: {hsl_to_hex(h, s, l)}')
