import os
from PIL import Image

def create_icon(filename, ascii_art, palette):
    img = Image.new('RGBA', (18, 18), (0,0,0,0))
    pixels = img.load()
    lines = [line.strip() for line in ascii_art.strip().split('\n')]
    for y, line in enumerate(lines):
        if y >= 18: break
        # padding left if line is too short
        line = line.ljust(18, '_')
        for x, char in enumerate(line):
            if x >= 18: break
            if char in palette:
                pixels[x, y] = palette[char]
    img.save(filename)
    print(f'Created {filename}')

palette = {
    '_': (0, 0, 0, 0),
    
    # Fire
    'R': (220, 20, 0, 255),    # Dark Fire Red
    'r': (255, 90, 0, 255),    # Fire Orange
    'y': (255, 200, 0, 255),   # Yellow
    
    # Ice
    'B': (0, 40, 220, 255),    # Dark Ice Blue
    'b': (0, 150, 255, 255),   # Ice Cyan
    'c': (160, 240, 255, 255), # Frost White
    
    # Lightning
    'P': (90, 0, 220, 255),    # Dark Volt Purple
    'p': (160, 40, 255, 255),  # Volt Magenta
    'm': (230, 130, 255, 255), # Volt Pink

    # Core
    'D': (30, 30, 30, 255),    # Obsidian Core
    'd': (80, 80, 80, 255),    # Ash Outline
    'W': (255, 255, 255, 255)  # White center
}

# Chaos I: Fire + Lightning (swirling orb)
chaos1 = '''
__________________
______rrrrrr______
____rryyyyyrrr____
___ryyy_R_RRr_r___
__ryy_R_mmp_R__r__
_ryy_R_mm__p_R__p_
_ry_R_mm____p_Rp_p
_r_R__m_W_W_m__p_p
_y_R_m_W_D_W_m_P_P
_y_R_m_W___W_m_P_P
_r_R__m_W_W_m__P_P
_ry_R_p____mm_P_P_
_rr__R_pp_mm_P_P__
__rr__R_Pmm_P_P___
___rrrP_PPP_PP____
____PPPPPPPPPP____
______PPPPPP______
__________________
'''

# Chaos II: Fire + Ice (crashing wave/crystals)
chaos2 = '''
__________________
______rrrrrr______
____rryyyyyrrr____
___ryyy_R_RRr_r___
__ryy_R_ccb_R__r__
_ryy_R_cc__b_R__b_
_ry_R_cc____b_Rb_b
_r_R__c_W_W_c__b_b
_y_R_c_W_D_W_c_B_B
_y_R_c_W___W_c_B_B
_r_R__c_W_W_c__B_B
_ry_R_b____cc_B_B_
_rr__R_bb_cc_B_B__
__rr__R_Bcc_B_B___
___rrrB_BBB_BB____
____BBBBBBBBBB____
______BBBBBB______
__________________
'''

# Chaos III: Ice + Lightning (resonating core)
chaos3 = '''
__________________
______bbbbbb______
____bbcccccbbb____
___bccc_B_BBb_b___
__bcc_B_mmp_B__b__
_bcc_B_mm__p_B__p_
_bc_B_mm____p_Bp_p
_b_B__m_W_W_m__p_p
_c_B_m_W_D_W_m_P_P
_c_B_m_W___W_m_P_P
_b_B__m_W_W_m__P_P
_bc_B_p____mm_P_P_
_bb__B_pp_mm_P_P__
__bb__B_Pmm_P_P___
___bbbP_PPP_PP____
____PPPPPPPPPP____
______PPPPPP______
__________________
'''

# Chaos Release: 3 elements spiraling into dark core
chaos_release = '''
__________________
______dP_P_P______
____d_Pp_PP_d_____
___d_B__P_Pd_r____
__d_BB_B_Pd_rr_d__
_d__BbbBBd_rrr_d__
_d_D__b_d_Rr_d_d__
_D__D_DpdPrDd_R_r_
_P_dDpdDDdRDd_y_r_
_p_PDpDDDDRyDR__r_
_pd_Pd_D_DRDd___r_
__pdDp_D_PrdDd_r__
___pdd___pd_ddR___
____pdpppddRrr____
_____pp_RrrR_d____
______pR_R_r______
__________________
__________________
'''

out_dir = r"e:\JMixin\Ice_and_Fire_RLCraft-1.20.1\src\main\resources\assets\iceandfire\textures\mob_effect"

create_icon(os.path.join(out_dir, 'chaos_i.png'), chaos1, palette)
create_icon(os.path.join(out_dir, 'chaos_ii.png'), chaos2, palette)
create_icon(os.path.join(out_dir, 'chaos_iii.png'), chaos3, palette)
create_icon(os.path.join(out_dir, 'chaos_release.png'), chaos_release, palette)
