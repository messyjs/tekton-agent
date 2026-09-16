import math, random

# ── mesh primitives ──────────────────────────────────────────────
def box(cx, cy, cz, sx, sy, sz, jit=0.0, seed=1, mat=0.6):
    rnd = random.Random(seed); V = []
    for dx in (-1, 1):
        for dy in (-1, 1):
            for dz in (-1, 1):
                j = lambda a: rnd.uniform(-a, a) if a else 0.0
                V.append(([cx+dx*sx/2+j(jit), cy+dy*sy/2+j(jit), cz+dz*sz/2+j(jit)], mat))
    F = [(0,2,3,1),(4,6,7,5),(0,1,5,4),(2,3,7,6),(0,4,6,2),(1,5,7,3)]
    T = []
    for f in F:
        a,b,c,d = f
        T.append(((V[a][0],V[b][0],V[c][0]), V[a][1])); T.append(((V[a][0],V[c][0],V[d][0]), V[a][1]))
    return T

def sphere(cx, cy, cz, r, mat=0.6, sx=1.0, sy=1.0, sz=1.0, n=7, m=5):
    T = []
    for i in range(n):
        for k in range(m):
            th0, th1 = i/n*2*math.pi, (i+1)/n*2*math.pi
            p0, p1 = k/m*math.pi, (k+1)/m*math.pi
            def pt(th, p):
                x, y, z = math.sin(p)*math.cos(th), math.cos(p), math.sin(p)*math.sin(th)
                return (cx+x*r*sx, cy+y*r*sy, cz+z*r*sz)
            a,b,c,d = pt(th0,p0), pt(th0,p1), pt(th1,p1), pt(th1,p0)
            T.append(((a,b,c), mat)); T.append(((a,c,d), mat))
    return T

def sword_tris():
    T = []
    T += box(0, 1.42, 0, 0.34, 0.18, 0.18, mat=0.9)          # crossguard
    T += box(0, 1.72, 0, 0.10, 0.46, 0.14, mat=0.95)           # grip
    T += sphere(0, 2.02, 0, 0.11, mat=0.85)                    # pommel
    T += box(0, 0.42, 0, 0.24, 1.72, 0.06, mat=1.0)          # blade
    tip = ((0,0.55-0.775,0),(0.08,-0.15,0.0225),(-0.08,-0.15,0.0225))
    T.append((( (0, -0.45, 0), (0.12, -0.42, 0.03), (-0.12, -0.42, 0.03) ), 1.0))
    return T

def scene():
    T = []
    T += box(0.3, -0.55, 0.0, 3.1, 1.2, 1.45, jit=0.09, seed=7, mat=0.62) # stone slab
    T += box(-1.15, -0.72, 0.55, 0.9, 0.55, 0.8, jit=0.15, seed=9, mat=0.58)  # stone chunk
    T += box(0.3, 0.62, 0.0, 0.26, 1.50, 0.06, mat=1.0)                  # blade
    T.append((( (0.3, -0.13, 0.0), (0.43, -0.10, 0.03), (0.17, -0.10, 0.03) ), 1.0))  # tip in stone
    T += box(0.3, 1.44, 0.0, 0.38, 0.16, 0.18, mat=0.9)                  # crossguard
    T += box(0.3, 1.72, 0.0, 0.11, 0.50, 0.14, mat=0.95)                 # grip
    T += sphere(0.3, 2.02, 0.0, 0.12, mat=0.85)                          # pommel
    hx, hy = -2.12, -0.30
    T += sphere(hx, hy, 0.95, 0.88, mat=0.78, sy=0.92, sz=1.04)          # helmet
    T += box(hx+0.70, hy-0.02, 0.95, 0.30, 0.18, 0.60, mat=0.06)         # visor slit
    T += box(hx+0.62, hy-0.44, 0.95, 0.24, 0.28, 0.55, jit=0.03, seed=3, mat=0.55)  # bevor
    for (px_,py_,pz_,r_) in [(-2.05,0.42,0.45,0.14),(-2.22,0.56,0.42,0.14),(-2.38,0.62,0.38,0.13),(-2.54,0.60,0.34,0.12)]:
        T += sphere(px_, py_, pz_+0.50, r_, mat=0.92)                          # plume arc
    T += sphere(hx-0.04, hy+0.66, 0.95, 0.16, mat=0.88)                   # plume root
    return T

# ── renderer ─────────────────────────────────────────────────────
RAMP = " .,:;=+*#%@@"
import sys
def g(v):
    return v ** 0.72
def render(T, W=104, HH=70, yaw=0.0, pitch=-0.06, out_h=35):
    cy_, sy_, = math.cos(yaw), math.sin(yaw)
    cp, sp = math.cos(pitch), math.sin(pitch)
    def xf(p):
        x, y, z = p
        x, z = x*cy_ + z*sy_, -x*sy_ + z*cy_
        y, z = y*cp - z*sp, y*sp + z*cp
        return x, y, z
    cam = 6.2
    P = []
    for tris, mat in T:
        vs = []
        n = None
        (a, b, c) = tris
        ax, ay, az = xf(a); bx, by, bz = xf(b); cx2, cy2, cz2 = xf(c)
        ux, uy, uz = bx-ax, by-ay, bz-az
        vx, vy, vz = cx2-ax, cy2-ay, cz2-az
        nx, ny, nz = uy*vz-uz*vy, uz*vx-ux*vz, ux*vy-uy*vx
        L = math.sqrt(nx*nx+ny*ny+nz*nz) or 1
        nx, ny, nz = nx/L, ny/L, nz/L
        if nz > 0: nx, ny, nz = -nx, -ny, -nz
        # two lights
        lx, ly, lz = -0.45, 0.80, 0.55
        l2x, l2y, l2z = 0.75, 0.15, 0.45
        d1 = max(0, -(nx*lx+ny*ly+nz*lz)); d2 = max(0, -(nx*l2x+ny*l2y+nz*l2z))
        shade = g(mat * (0.20 + 0.78*d1 + 0.22*d2))
        pts = []
        for (px, py, pz) in ((ax,ay,az),(bx,by,bz),(cx2,cy2,cz2)):
            s = cam/(cam+pz) if pz > -4.5 else 99
            pts.append((W/2 + (px+0.30)*s*W/4.4, HH/2 - (py-0.45)*s*W/4.4*0.55, pz))
        P.append((pts, min(99, shade)))
    Z = [[(1e9, -1)]*W for _ in range(HH)]
    for pts, sh in P:
        (x0,y0,z0),(x1,y1,z1),(x2,y2,z2) = pts
        if 99 in (z0,z1,z2): continue
        minx, maxx = max(0,int(min(x0,x1,x2))), min(W-1,int(max(x0,x1,x2))+1)
        miny, maxy = max(0,int(min(y0,y1,y2))), min(HH-1,int(max(y0,y1,y2))+1)
        if minx>maxx or miny>maxy: continue
        d = (x1-x0)*(y2-y0)-(x2-x0)*(y1-y0)
        if abs(d) < 1e-9: continue
        for yy in range(miny, maxy+1):
            for xx in range(minx, maxx+1):
                w0 = ((x1-xx)*(y2-yy)-(x2-xx)*(y1-yy))/d
                w1 = ((x2-xx)*(y0-yy)-(x0-xx)*(y2-yy))/d
                w2 = 1-w0-w1
                if w0<-0.02 or w1<-0.02 or w2<-0.02: continue
                z = w0*z0+w1*z1+w2*z2
                if z < Z[yy][xx][0]: Z[yy][xx] = (z, sh)
    lines = []
    for ry in range(out_h):
        line = ""
        for xx in range(W):
            a = Z[ry*2][xx][1]; b = Z[ry*2+1][xx][1]
            v = max(a, b) if max(a,b) >= 0 else -1
            line += " " if v < 0 else RAMP[min(len(RAMP)-1, int(v*(len(RAMP)-1)))]
        lines.append(line.rstrip())
    return "\n".join(lines)

T = scene()
art = render(T, yaw=0.38, W=88, HH=62, out_h=31)
open("final.txt","w").write(art + "\n")
ys = [0.10,0.20,0.30,0.38,0.46,0.56,0.66,0.56,0.46,0.38]
frs = [render(T, yaw=y, W=88, HH=62, out_h=31) for y in ys]
import json
open("frames.json","w").write(json.dumps(frs))
print("frames:", len(frs))
open("frame.txt","w").write(art+"\n")
print(art)
