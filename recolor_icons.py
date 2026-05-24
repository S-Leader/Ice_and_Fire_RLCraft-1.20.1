import os
import math
from PIL import Image

def color_blend(c1, c2, t):
    """Blend two colors (r, g, b) by parameter t [0..1]"""
    t = max(0.0, min(1.0, t))
    return (
        int(c1[0] * (1-t) + c2[0] * t),
        int(c1[1] * (1-t) + c2[1] * t),
        int(c1[2] * (1-t) + c2[2] * t)
    )

def recolor(img_path, out_path, color_funcs):
    # Load base image
    try:
        base = Image.open(img_path).convert('RGBA')
    except Exception as e:
        print("Failed to open base image:", e)
        return

    width, height = base.size
    pixels = base.load()
    
    out = Image.new('RGBA', (width, height))
    out_pixels = out.load()
    
    cx, cy = width / 2.0, height / 2.0
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
                
            # Angle around center (-pi to pi)
            dx = x - cx + 0.5
            dy = y - cy + 0.5
            dist = math.hypot(dx, dy)
            angle = math.atan2(dy, dx)
            
            # Map angle to 0.0 - 1.0
            normalized_angle = (angle + math.pi) / (2 * math.pi)
            
            # Get new color based on angle and dist
            new_r, new_g, new_b = color_funcs(normalized_angle, dist)
            
            # Preserve original alpha and boost brightness to make it look emissive
            orig_brightness = (r + g + b) / 3.0 / 255.0
            lum_factor = 0.6 + orig_brightness * 3.0
            
            out_pixels[x, y] = (
                int(min(255, new_r * lum_factor)),
                int(min(255, new_g * lum_factor)),
                int(min(255, new_b * lum_factor)),
                a
            )
            
    out.save(out_path)
    print("Saved", out_path)


# Core palettes (Light -> Dark)
# Fire: Bright Orange -> Dark Red
FIRE_L = (255, 140, 0)
FIRE_D = (200, 20, 0)

# Ice: Cyan -> Dark Blue
ICE_L = (0, 220, 255)
ICE_D = (0, 40, 200)

# Lightning: Magenta -> Dark Purple
LIGHT_L = (220, 100, 255)
LIGHT_D = (110, 0, 220)

def get_gradient_2(t, dist, c1_light, c1_dark, c2_light, c2_dark):
    # Determine side based on angle t
    mix_t = math.sin(t * math.pi * 2) * 0.5 + 0.5
    
    # Distance based shading (center is lighter, edges are darker)
    dist_t = min(1.0, dist / 8.0)
    
    side1 = color_blend(c1_light, c1_dark, dist_t)
    side2 = color_blend(c2_light, c2_dark, dist_t)
    
    return color_blend(side1, side2, mix_t)

def get_gradient_3(t, dist):
    dist_t = min(1.0, dist / 8.0)
    c_fire = color_blend(FIRE_L, FIRE_D, dist_t)
    c_ice = color_blend(ICE_L, ICE_D, dist_t)
    c_light = color_blend(LIGHT_L, LIGHT_D, dist_t)
    
    if t < 0.333:
        local_t = t / 0.333
        return color_blend(c_fire, c_ice, local_t)
    elif t < 0.666:
        local_t = (t - 0.333) / 0.333
        return color_blend(c_ice, c_light, local_t)
    else:
        local_t = (t - 0.666) / 0.333
        return color_blend(c_light, c_fire, local_t)

base_img = r'D:\123\603336.png'
out_dir = r"e:\JMixin\Ice_and_Fire_RLCraft-1.20.1\src\main\resources\assets\iceandfire\textures\mob_effect"

recolor(base_img, os.path.join(out_dir, 'chaos_i.png'), lambda t, d: get_gradient_2(t, d, FIRE_L, FIRE_D, LIGHT_L, LIGHT_D))
recolor(base_img, os.path.join(out_dir, 'chaos_ii.png'), lambda t, d: get_gradient_2(t, d, FIRE_L, FIRE_D, ICE_L, ICE_D))
recolor(base_img, os.path.join(out_dir, 'chaos_iii.png'), lambda t, d: get_gradient_2(t, d, ICE_L, ICE_D, LIGHT_L, LIGHT_D))
recolor(base_img, os.path.join(out_dir, 'chaos_release.png'), lambda t, d: get_gradient_3(t, d))

print("All icons recolored successfully.")
