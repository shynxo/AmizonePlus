"""
Pixel-art source for the app's pets (cat + dog).

Run:  python tools/pets/sprites.py
  -> writes tools/pets/preview_cat.png, preview_dog.png (8x scale, for checking)
  -> writes app/src/main/java/com/student/amizoneplus/ui/pets/PetSprites.kt

Every frame is a CANVAS_W x CANVAS_H grid of palette letters ('.' = empty),
facing RIGHT. The app flips frames for facing left. Frames are assembled from
parts (head / body / legs / tail) so poses stay consistent.

Anchors: standing frames have their feet at the bottom row, centred on FOOT_X.
Hanging / held frames have their grip point at the top row, centred on GRIP_X.
"""
import os, struct, zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
OUT_KT = os.path.join(ROOT, 'app/src/main/java/com/student/amizoneplus/ui/pets/PetSprites.kt')

CANVAS_W, CANVAS_H = 26, 22
FOOT_X = 13       # feet / grip centre column
GRIP_X = 13

# ───────────────────────── helpers ─────────────────────────

def blank():
    return [['.'] * CANVAS_W for _ in range(CANVAS_H)]

def paste(c, part, x, y):
    for r, line in enumerate(part):
        for k, ch in enumerate(line):
            if ch in '. ':
                continue
            yy, xx = y + r, x + k
            if 0 <= yy < CANVAS_H and 0 <= xx < CANVAS_W:
                c[yy][xx] = ch
    return c

def frame(*layers):
    c = blank()
    for part, x, y in layers:
        paste(c, part, x, y)
    return [''.join(r) for r in c]

def check(name, part):
    w = len(part[0])
    for i, l in enumerate(part):
        assert len(l) == w, f'{name} row {i} is {len(l)} wide, expected {w}: {l!r}'
    return part

def sub(part, row, text):
    """Replace one row of a part (for eye / mouth variants)."""
    p = list(part); p[row] = text; return check('sub', p)

# ───────────────────────── CAT ─────────────────────────
# Palette: K outline, W white fur, G soft shade, P pink, N nose, E eye, H eye shine, O orange patch
CAT_PAL = {
    'K': 0xFF2B2433, 'W': 0xFFFFFFFF, 'G': 0xFFDCD6EA, 'P': 0xFFF7A8B8,
    'N': 0xFFF07C93, 'E': 0xFF2B2433, 'H': 0xFFFFFFFF, 'O': 0xFFF4B26A,
}

C_HEAD = check('C_HEAD', [
    ".K.......K.",
    "KPK.....KPK",
    "KWOKKKKKWWK",
    "KWOOWWWWWWK",
    "KWEHWWWEHWK",
    "KWEEWWWEEWK",
    "KPWWWNWWWPK",
    ".KWWKWKWWK.",
    "..KKKKKKK..",
])
C_HEAD_BLINK = sub(sub(C_HEAD, 4, "KWWWWWWWWWK"), 5, "KWKKWWWKKWK")
C_HEAD_HAPPY = sub(sub(C_HEAD, 4, "KWKKWWWKKWK"), 5, "KWWWWWWWWWK")
C_HEAD_UP = sub(sub(sub(C_HEAD, 3, "KWEHWWWEHWK"), 4, "KWEEWWWEEWK"), 5, "KWWWWWWWWWK")
C_HEAD_YAWN = sub(sub(sub(C_HEAD_BLINK, 6, "KPWWKKKWWPK"), 7, ".KWWKNKWWK."), 8, "..KKKKKKK..")
C_HEAD_MEOW = sub(sub(C_HEAD, 6, "KPWWWNWWWPK"), 7, ".KWWKNKWWK.")
C_HEAD_WIDE = sub(sub(C_HEAD, 4, "KEHWWWWWEHK"), 5, "KEEWWWWWEEK")  # startled

C_BODY = check('C_BODY', [
    "..KKKKKKKKK..",
    ".KWWWWWWWWWK.",
    "KWWWWWWWWWWWK",
    "KWWWWWWWWWWWK",
    "KGWWWWWWWWWGK",
    ".KGGWWWWWGGK.",
    "..KKKKKKKKK..",
])
# legs under the body (13 wide, 3 tall) — walk cycle A B C B
C_LEGS_STAND = check('C_LEGS_STAND', ["..KWK...KWK..", "..KWK...KWK..", "..KKK...KKK.."])
C_LEGS_A = check('C_LEGS_A', ["..KWK....KWK.", ".KWK......KWK", ".KK........KK"])
C_LEGS_B = check('C_LEGS_B', ["...KWK.KWK...", "...KWK.KWK...", "...KK..KK...."])
C_LEGS_C = check('C_LEGS_C', ["..KWK...KWK..", "...KWKKWK....", "....KKKK....."])
C_LEGS_RUN1 = check('C_LEGS_RUN1', [".KWK......KWK", "KWK........KW", "KK..........K"])
C_LEGS_RUN2 = check('C_LEGS_RUN2', ["....KWKKWK...", "....KWKKWK...", "....KK.KK...."])
C_LEGS_CROUCH = check('C_LEGS_CROUCH', [".KKWK...KWKK."])

C_TAIL_UP = check('C_TAIL_UP', [
    ".KK..",
    "KOOK.",
    "KWK..",
    "KWK..",
    ".KWK.",
    ".KWK.",
    "..KWK",
    "...KW",
])
C_TAIL_MID = check('C_TAIL_MID', [
    ".....",
    "..KK.",
    ".KOOK",
    "KWK..",
    "KWK..",
    ".KWK.",
    "..KWK",
    "...KW",
])
C_TAIL_SWAY = check('C_TAIL_SWAY', [
    "KK...",
    "KOK..",
    ".KWK.",
    ".KWK.",
    ".KWK.",
    ".KWK.",
    "..KWK",
    "...KW",
])
C_TAIL_FLAT = check('C_TAIL_FLAT', ["KKKK.", "KOWWK", ".KKKK"])

def cat_stand(head=C_HEAD, legs=C_LEGS_STAND, tail=C_TAIL_UP, bob=0, head_dy=0):
    by = 12 + bob
    return frame(
        (tail, 1, by - 7),
        (C_BODY, 4, by),
        (legs, 4, by + 7),
        (head, 13, by - 6 + head_dy),
    )

C_SIT_BODY = check('C_SIT_BODY', [
    "..KKKKK..",
    ".KWWWWWK.",
    "KWWWWWWWK",
    "KWWWWWWWK",
    "KWWWWWWWK",
    "KGWWWWWGK",
    "KGWWKWWGK",
    "KGWKWKWGK",
    ".KKK.KKK.",
])
C_SIT_TAIL = check('C_SIT_TAIL', ["...KKK", "..KOOK", "KKWWK.", "KWWK..", ".KK..."])
C_SIT_TAIL2 = check('C_SIT_TAIL2', ["......", "...KKK", "KKKOOK", "KWWWK.", ".KKK.."])
C_PAW_UP = check('C_PAW_UP', [".KK", "KWK", "KWK"])  # raised paw for grooming / waving

def cat_sit(head=C_HEAD, tail=C_SIT_TAIL, paw=False, head_dx=0, head_dy=0):
    parts = [(tail, 4, 15), (C_SIT_BODY, 8, 13), (head, 7 + head_dx, 5 + head_dy)]
    if paw:
        parts.append((C_PAW_UP, 14, 11))
    return frame(*parts)

C_LOAF = check('C_LOAF', [
    "...KKKKKKKKK...",
    "..KWWWWWWWWWK..",
    ".KWWWWWWWWWWWK.",
    "KWWWWWWWWWWWWWK",
    "KGWWWWWWWWWWWGK",
    ".KKKKKKKKKKKKK.",
])
C_LOAF_PAWS1 = check('C_LOAF_PAWS1', ["KWK.KWK", "KKK.KKK"])
C_LOAF_PAWS2 = check('C_LOAF_PAWS2', [".......", "KWK.KWK"])

def cat_loaf(head=C_HEAD_BLINK, breathe=0, paws=None, tail=C_TAIL_FLAT):
    y = 16 - breathe
    parts = [(tail, 1, 18), (C_LOAF, 4, y), (head, 12, y - 5 + breathe)]
    if paws:
        parts.append((paws, 15, 20))
    return frame(*parts)

# crouch (pounce wind-up): low body, butt up a little
C_CROUCH_BODY = check('C_CROUCH_BODY', [
    "KKKKKKKK.....",
    "KWWWWWWWKKKK.",
    "KWWWWWWWWWWWK",
    "KGWWWWWWWWWGK",
    ".KKKKKKKKKKK.",
])

def cat_crouch(tail=C_TAIL_SWAY, wiggle=0):
    return frame((tail, 0 + wiggle, 9), (C_CROUCH_BODY, 4 + wiggle, 16), (C_LEGS_CROUCH, 4 + wiggle, 21), (C_HEAD_WIDE, 13, 11))

# stretch: front down, butt up
C_STRETCH = check('C_STRETCH', [
    "KKKK...........",
    "KWWWKK.........",
    "KWWWWWKKK......",
    "KWWWWWWWWKKK...",
    ".KGWWWWWWWWWK..",
    "..KWK..KKKKKKKK",
    "..KWK...KWWWWWK",
    "..KKK...KKKKKKK",
])

def cat_stretch():
    return frame((C_TAIL_UP, 1, 6), (C_STRETCH, 4, 14), (C_HEAD_BLINK, 14, 12))

# airborne
def cat_jump_up():
    return frame((C_TAIL_MID, 0, 7), (C_BODY, 4, 11), (C_LEGS_RUN1, 4, 18), (C_HEAD_UP, 13, 5))

def cat_fall():
    return frame((C_TAIL_SWAY, 1, 6), (C_BODY, 4, 11), (C_LEGS_RUN1, 4, 18), (C_HEAD_WIDE, 13, 5))

# hanging by the front paws (grip at top row, centre GRIP_X)
C_ARM = check('C_ARM', [".KK.", "KWWK", "KWK.", "KWK.", "KWK."])
C_HANG_BODY = check('C_HANG_BODY', [
    ".KKKKKKK.",
    "KWWWWWWWK",
    "KWWWWWWWK",
    "KWWWWWWWK",
    "KGWWWWWGK",
    ".KGWWWGK.",
    "..KKKKK..",
])
C_HANG_LEGS1 = check('C_HANG_LEGS1', ["KWK.KWK", "KWK.KWK", "KKK.KKK"])
C_HANG_LEGS2 = check('C_HANG_LEGS2', ["KWK.KWK", ".KWKWK.", ".KK.KK."])
C_HANG_TAIL = check('C_HANG_TAIL', [".KWK", ".KWK", "KWK.", "KOK.", ".KK."])

def cat_hang(legs=C_HANG_LEGS1, head=C_HEAD_UP):
    return frame((head, 8, 3), (C_ARM, 7, 0), (C_ARM, 16, 0), (C_HANG_BODY, 9, 11), (legs, 10, 18), (C_HANG_TAIL, 16, 16))

# held by the scruff (grip point = top of head)
def cat_held(legs=C_HANG_LEGS1, head=C_HEAD_WIDE):
    return frame((head, 8, 0), (C_HANG_BODY, 9, 8), (legs, 10, 15), (C_HANG_TAIL, 16, 13),
                 (["KK", "KK"], 9, 9), (["KK", "KK"], 16, 9))

CAT = {
    'idle0': cat_stand(tail=C_TAIL_UP),
    'idle1': cat_stand(tail=C_TAIL_MID),
    'idle2': cat_stand(tail=C_TAIL_SWAY),
    'idle_blink': cat_stand(head=C_HEAD_BLINK),
    'idle_happy': cat_stand(head=C_HEAD_HAPPY, tail=C_TAIL_MID),
    'idle_up': cat_stand(head=C_HEAD_UP),
    'walk0': cat_stand(legs=C_LEGS_A, tail=C_TAIL_MID),
    'walk1': cat_stand(legs=C_LEGS_B, tail=C_TAIL_MID, bob=-1),
    'walk2': cat_stand(legs=C_LEGS_C, tail=C_TAIL_UP),
    'walk3': cat_stand(legs=C_LEGS_B, tail=C_TAIL_UP, bob=-1),
    'run0': cat_stand(legs=C_LEGS_RUN1, tail=C_TAIL_SWAY, bob=-1),
    'run1': cat_stand(legs=C_LEGS_RUN2, tail=C_TAIL_MID, bob=0),
    'sit0': cat_sit(),
    'sit1': cat_sit(tail=C_SIT_TAIL2),
    'sit_blink': cat_sit(head=C_HEAD_BLINK),
    'sit_up': cat_sit(head=C_HEAD_UP),
    'sit_happy': cat_sit(head=C_HEAD_HAPPY, tail=C_SIT_TAIL2),
    'yawn': cat_sit(head=C_HEAD_YAWN),
    'meow': cat_sit(head=C_HEAD_MEOW),
    'groom0': cat_sit(head=C_HEAD_BLINK, paw=True, head_dx=1, head_dy=1),
    'groom1': cat_sit(head=C_HEAD_BLINK, paw=True, head_dx=2, head_dy=2),
    'loaf0': cat_loaf(),
    'loaf1': cat_loaf(breathe=1),
    'loaf_awake': cat_loaf(head=C_HEAD),
    'knead0': cat_loaf(head=C_HEAD_HAPPY, paws=C_LOAF_PAWS1),
    'knead1': cat_loaf(head=C_HEAD_HAPPY, paws=C_LOAF_PAWS2),
    'crouch0': cat_crouch(wiggle=0),
    'crouch1': cat_crouch(tail=C_TAIL_MID, wiggle=1),
    'stretch': cat_stretch(),
    'jump': cat_jump_up(),
    'fall': cat_fall(),
    'hang0': cat_hang(),
    'hang1': cat_hang(legs=C_HANG_LEGS2),
    'hang_happy': cat_hang(head=C_HEAD_HAPPY),
    'held0': cat_held(),
    'held1': cat_held(legs=C_HANG_LEGS2),
}

# ───────────────────────── DOG ─────────────────────────
# Palette: K outline, T tan, B brown ears/saddle, W white, N nose, P tongue pink, E eye, H shine, R red collar, Y tag
DOG_PAL = {
    'K': 0xFF2B2433, 'T': 0xFFE6B27E, 'B': 0xFFA8693F, 'W': 0xFFFFF8F0, 'N': 0xFF2B2433,
    'P': 0xFFF27C8E, 'E': 0xFF2B2433, 'H': 0xFFFFFFFF, 'R': 0xFFE2483D, 'Y': 0xFFF7C54A, 'G': 0xFFC9925F,
}

D_HEAD = check('D_HEAD', [
    "....KKKKKKK....",
    "...KTTTWTTTK...",
    ".KKTTTTWTTTTKK.",
    "KBBKTTTWTTTKBBK",
    "KBBKTEHWEHTKBBK",
    "KBBKTEEWEETKBBK",
    "KBBKTWWNWWTKBBK",
    ".KBKTWKWKWTKBK.",
    "..KKKKWWWKKKK..",
    "......KKK......",
])
D_HEAD_BLINK = sub(sub(D_HEAD, 4, "KBBKTTTWTTTKBBK"), 5, "KBBKTKKWKKTKBBK")
D_HEAD_HAPPY = sub(sub(sub(D_HEAD, 4, "KBBKTKKWKKTKBBK"), 5, "KBBKTTTWTTTKBBK"), 8, "..KKKKPPPKKKK..")
D_HEAD_TONGUE = sub(sub(D_HEAD, 8, "..KKKKWPWKKKK.."), 9, "......KPK......")
D_HEAD_BARK = sub(sub(D_HEAD, 7, ".KBKTWKKKWTKBK."), 8, "..KKKKKPKKKKK..")
D_HEAD_UP = sub(sub(sub(D_HEAD, 3, "KBBKTEHWEHTKBBK"), 4, "KBBKTEEWEETKBBK"), 5, "KBBKTTTWTTTKBBK")
D_HEAD_SNIFF = check('D_HEAD_SNIFF', [  # side profile, nose down
    "..KKKKK.....",
    ".KTTTTTKK...",
    "KBBTTEHTTK..",
    "KBBTTEETTTK.",
    "KBBKTTTTTTTK",
    ".KBKTTTWWWWN",
    "..KK.KKKKKKK",
])

D_BODY = check('D_BODY', [
    "..KKKKKKKKKKK..",
    ".KTTBBBBBTTTTK.",
    "KTTBBBBBBBTTTTK",
    "KTTTBBBBBTTTTTK",
    "KGTTTTTTTTTWWWK",
    ".KGGWWWWWWWWWK.",
    "..KKKKKKKKKKK..",
])
D_LEGS_STAND = check('D_LEGS_STAND', ["..KTK.....KTK..", "..KTK.....KTK..", ".KKWK....KKWK.."])
D_LEGS_A = check('D_LEGS_A', ["..KTK......KTK.", ".KTK........KTK", "KKW.........KWK"])
D_LEGS_B = check('D_LEGS_B', ["...KTK...KTK...", "...KTK...KTK...", "..KKW...KKW...."])
D_LEGS_C = check('D_LEGS_C', ["..KTK.....KTK..", "...KTK..KTK....", "...KKW.KKW....."])
D_LEGS_RUN1 = check('D_LEGS_RUN1', [".KTK.......KTK.", "KTK.........KTK", "KW...........KW"])
D_LEGS_RUN2 = check('D_LEGS_RUN2', ["....KTK.KTK....", "....KTKKTK.....", "....KWKKW......"])
D_COLLAR = check('D_COLLAR', ["KRRRRRK", ".KRYRK."])

D_TAIL_UP = check('D_TAIL_UP', ["K...", "KK..", "KTK.", ".KTK", "..KT"])
D_TAIL_L = check('D_TAIL_L', ["KK..", "KTK.", ".KTK", ".KTK", "..KT"])
D_TAIL_R = check('D_TAIL_R', ["..KK", ".KTK", ".KTK", ".KTK", "..KT"])
D_TAIL_DOWN = check('D_TAIL_DOWN', ["....", "...K", "..KT", ".KTK", "KTK."])

def dog_stand(head=D_HEAD, legs=D_LEGS_STAND, tail=D_TAIL_UP, bob=0, head_dy=0):
    by = 12 + bob
    return frame(
        (tail, 1, by - 4),
        (D_BODY, 3, by),
        (legs, 3, by + 7),
        (head, 12, by - 7 + head_dy),
        (D_COLLAR, 16, by + 2 + head_dy),
    )

D_SIT_BODY = check('D_SIT_BODY', [
    "..KKKKKKK..",
    ".KTTTTTTTK.",
    "KTTTWWWTTTK",
    "KTTWWWWWTTK",
    "KBTWWWWWTBK",
    "KBTTWWWTTBK",
    "KBTKTTTKTBK",
    "KBTKTKTKTBK",
    ".KKWK.KWKK.",
])

def dog_sit(head=D_HEAD, tail=D_TAIL_L, scratch=None):
    parts = [(tail, 3, 14), (D_SIT_BODY, 7, 13), (head, 5, 4), (D_COLLAR, 9, 13)]
    if scratch is not None:
        parts.append(scratch)
    return frame(*parts)

D_LIE = check('D_LIE', [
    "..KKKKKKKKKKKKK..",
    ".KTTBBBBBBTTTTTK.",
    "KTTBBBBBBBBTTTTTK",
    "KGTTTTTTTTTTTTWWK",
    ".KKKKKKKKKKKKKKKK",
])
D_PAWS_FRONT = check('D_PAWS_FRONT', ["KTTK.KTTK", "KWWK.KWWK", ".KK...KK."][0:2])

def dog_lie(head=D_HEAD_BLINK, breathe=0, tail=D_TAIL_DOWN):
    y = 17 - breathe
    return frame((tail, 0, y - 1), (D_LIE, 2, y), (head, 12, y - 7 + breathe), (D_PAWS_FRONT, 15, 20))

D_BELLY = check('D_BELLY', [  # rolled over, legs up
    "..KTK..KTK......",
    "..KTK..KTK......",
    ".KKKKKKKKKKKK...",
    "KWWWWWWWWWWWWK..",
    "KTTTTTTTTTTTTTK.",
    ".KKKKKKKKKKKKKK.",
])
D_BELLY_LEGS2 = check('D_BELLY_LEGS2', ["...KTK..KTK.....", "..KTK..KTK......"])

def dog_belly(alt=False):
    parts = [(D_BELLY, 2, 16), (D_HEAD_HAPPY, 12, 12), (D_TAIL_DOWN, 0, 17)]
    if alt:
        parts.append((D_BELLY_LEGS2, 2, 16))
    return frame(*parts)

def dog_dig(alt=False):
    legs = D_LEGS_A if alt else D_LEGS_C
    return frame((D_TAIL_UP, 1, 6), (D_BODY, 3, 11), (legs, 3, 18), (D_HEAD_SNIFF, 14, 15))

def dog_sniff(alt=False):
    return frame((D_TAIL_L if alt else D_TAIL_R, 1, 8), (D_BODY, 3, 12), (D_LEGS_STAND, 3, 19), (D_HEAD_SNIFF, 14, 15 + (1 if alt else 0)))

def dog_bow(alt=False):  # play bow: front down, butt up, tail wag
    return frame((D_TAIL_L if alt else D_TAIL_R, 0, 6), (D_BODY, 3, 11), (D_LEGS_STAND, 3, 18), (D_HEAD_TONGUE, 12, 11))

def dog_jump():
    return frame((D_TAIL_UP, 1, 6), (D_BODY, 3, 10), (D_LEGS_RUN1, 3, 17), (D_HEAD_UP, 12, 3), (D_COLLAR, 16, 12))

def dog_held(alt=False):
    legs = check('dl', ["KTK.KTK", "KTK.KTK", "KWK.KWK"]) if not alt else check('dl2', ["KTK.KTK", ".KTKTK.", ".KW.WK."])
    body = check('dhb', [".KKKKKKKK.", "KTTBBBBTTK", "KTBBBBBBTK", "KTTWWWWTTK", "KTTWWWWTTK", ".KTTTTTTK.", "..KKKKKK.."])
    return frame((D_HEAD_BLINK if alt else D_HEAD_UP, 6, 0), (body, 8, 9), (legs, 9, 16), (D_TAIL_DOWN, 16, 13), (D_COLLAR, 10, 9))

SCRATCH1 = (check('sc1', ["KTK", "KTK", "KW."]), 15, 10)
SCRATCH2 = (check('sc2', ["KTK", "KTK", ".KW"]), 15, 11)

DOG = {
    'idle0': dog_stand(tail=D_TAIL_UP),
    'idle1': dog_stand(tail=D_TAIL_L),
    'idle_blink': dog_stand(head=D_HEAD_BLINK),
    'idle_tongue': dog_stand(head=D_HEAD_TONGUE, tail=D_TAIL_R),
    'idle_up': dog_stand(head=D_HEAD_UP),
    'wag0': dog_stand(head=D_HEAD_TONGUE, tail=D_TAIL_L),
    'wag1': dog_stand(head=D_HEAD_TONGUE, tail=D_TAIL_R),
    'walk0': dog_stand(legs=D_LEGS_A, tail=D_TAIL_L),
    'walk1': dog_stand(legs=D_LEGS_B, tail=D_TAIL_UP, bob=-1),
    'walk2': dog_stand(legs=D_LEGS_C, tail=D_TAIL_R),
    'walk3': dog_stand(legs=D_LEGS_B, tail=D_TAIL_UP, bob=-1),
    'run0': dog_stand(head=D_HEAD_TONGUE, legs=D_LEGS_RUN1, tail=D_TAIL_L, bob=-1),
    'run1': dog_stand(head=D_HEAD_TONGUE, legs=D_LEGS_RUN2, tail=D_TAIL_R),
    'sit0': dog_sit(),
    'sit1': dog_sit(tail=D_TAIL_R),
    'sit_blink': dog_sit(head=D_HEAD_BLINK),
    'sit_happy': dog_sit(head=D_HEAD_HAPPY, tail=D_TAIL_R),
    'sit_tongue': dog_sit(head=D_HEAD_TONGUE),
    'sit_up': dog_sit(head=D_HEAD_UP),
    'bark0': dog_sit(head=D_HEAD_BARK),
    'bark1': dog_stand(head=D_HEAD_BARK, tail=D_TAIL_UP),
    'scratch0': dog_sit(head=D_HEAD_HAPPY, scratch=SCRATCH1),
    'scratch1': dog_sit(head=D_HEAD_HAPPY, scratch=SCRATCH2),
    'lie0': dog_lie(),
    'lie1': dog_lie(breathe=1),
    'lie_awake': dog_lie(head=D_HEAD),
    'belly0': dog_belly(),
    'belly1': dog_belly(alt=True),
    'dig0': dog_dig(),
    'dig1': dog_dig(alt=True),
    'sniff0': dog_sniff(),
    'sniff1': dog_sniff(alt=True),
    'bow0': dog_bow(),
    'bow1': dog_bow(alt=True),
    'jump': dog_jump(),
    'fall': dog_jump(),
    'held0': dog_held(),
    'held1': dog_held(alt=True),
}

# ───────────────────────── preview + export ─────────────────────────

def write_png(path, pixels, w, h):
    raw = b''.join(b'\x00' + bytes(pixels[y * w * 4:(y + 1) * w * 4]) for y in range(h))
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xFFFFFFFF)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + \
        chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    open(path, 'wb').write(png)

def preview(frames, pal, path, scale=6, cols=6, bg=(0x6F, 0x73, 0xF4)):
    names = list(frames)
    rows = (len(names) + cols - 1) // cols
    cw, ch = CANVAS_W * scale + 8, CANVAS_H * scale + 8
    W, H = cols * cw, rows * ch
    px = bytearray(W * H * 4)
    for i in range(W * H):
        px[i * 4:i * 4 + 4] = bytes((bg[0], bg[1], bg[2], 255))
    for n, name in enumerate(names):
        ox, oy = (n % cols) * cw + 4, (n // cols) * ch + 4
        for y, line in enumerate(frames[name]):
            for x, c in enumerate(line):
                if c == '.':
                    continue
                v = pal[c]
                col = ((v >> 16) & 255, (v >> 8) & 255, v & 255, 255)
                for dy in range(scale):
                    for dx in range(scale):
                        p = ((oy + y * scale + dy) * W + ox + x * scale + dx) * 4
                        px[p:p + 4] = bytes(col)
        # foot marker
        for dx in range(scale):
            p = ((oy + CANVAS_H * scale + 1) * W + ox + FOOT_X * scale + dx) * 4
            px[p:p + 4] = bytes((255, 255, 0, 255))
    write_png(path, px, W, H)
    print(path, 'order:', ', '.join(names))

def kt_frames(name, frames):
    out = [f'    val {name}: Map<String, Array<String>> = mapOf(']
    for k, rows in frames.items():
        body = ', '.join('"' + r + '"' for r in rows)
        out.append(f'        "{k}" to arrayOf({body}),')
    out.append('    )')
    return '\n'.join(out)

def kt_pal(name, pal):
    items = ', '.join(f"'{k}' to 0x{v:08X}.toInt()" for k, v in pal.items())
    return f'    val {name}: Map<Char, Int> = mapOf({items})'

def export():
    os.makedirs(os.path.dirname(OUT_KT), exist_ok=True)
    src = f'''package com.student.amizoneplus.ui.pets

// GENERATED by tools/pets/sprites.py — edit the Python, not this file.
// Frames are {CANVAS_W}x{CANVAS_H} pixel grids facing right; '.' is transparent.

object PetSprites {{
    const val W = {CANVAS_W}
    const val H = {CANVAS_H}
    const val FOOT_X = {FOOT_X}
    const val GRIP_X = {GRIP_X}

{kt_pal('CAT_PALETTE', CAT_PAL)}
{kt_pal('DOG_PALETTE', DOG_PAL)}

{kt_frames('CAT', CAT)}

{kt_frames('DOG', DOG)}
}}
'''
    open(OUT_KT, 'w', encoding='utf-8').write(src)
    print('wrote', OUT_KT)

if __name__ == '__main__':
    preview(CAT, CAT_PAL, os.path.join(HERE, 'preview_cat.png'))
    preview(DOG, DOG_PAL, os.path.join(HERE, 'preview_dog.png'))
    export()
