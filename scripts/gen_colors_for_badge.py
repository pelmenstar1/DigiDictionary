import colorsys

COLOR_COUNT = 30
HUE_STEP = 1 / COLOR_COUNT


def rgb_comp_hex(value: float) -> str:
    result = hex(int(value * 255.0))[2:]

    if len(result) == 1:
        result = '0' + result

    return result


print("<array name=\"add_edit_badge_dialog_outline_colors\">")

for i in range(COLOR_COUNT):
    h = HUE_STEP * i
    l = 0.5
    s = 1

    r, g, b = colorsys.hls_to_rgb(h, l, s)
    rgb_str = f'#{rgb_comp_hex(r)}{rgb_comp_hex(g)}{rgb_comp_hex(b)}'

    print(f"    <item>{rgb_str}</item>")

print("</array>")
