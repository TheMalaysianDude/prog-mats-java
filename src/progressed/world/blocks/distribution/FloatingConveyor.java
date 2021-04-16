package progressed.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class FloatingConveyor extends Conveyor{
    private static final float itemSpace = 0.4f;

    final Vec2 tr1 = new Vec2();
    final Vec2 tr2 = new Vec2();

    public boolean drawShallow;
    public float coverOpacity = 0.5f;
    public float deepSpeed = -1f;
    public float deepDisplayedSpeed = 0f;

    protected TextureRegion[] topRegions = new TextureRegion[5], coverRegions = new TextureRegion[5];

    public FloatingConveyor(String name){
        super(name);
        floating = true;
        placeableLiquid = true;
    }

    @Override
    public void init(){
        super.init();

        if(deepSpeed < 0f) deepSpeed = speed;
    }

    @Override
    public void load(){
        super.load();
        for(int i = 0; i < 5; i++){
            topRegions[i] = Core.atlas.find(name + "-top-" + i);
            coverRegions[i] = Core.atlas.find(name + "-cover-" + i);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemsMoved, "(" + deepDisplayedSpeed + " " + StatUnit.itemsSecond.localized() + " when on deep liquid)");
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        super.drawRequestRegion(req, list);
        if(checkDeep(req.drawx(), req.drawy())){
            int[] bits = getTiling(req, list);

            if(bits == null) return;

            Draw.color(world.tileWorld(req.drawx(), req.drawy()).floor().mapColor, coverOpacity);
            TextureRegion region = coverRegions[bits[0]];
            Draw.rect(region, req.drawx(), req.drawy(), region.width * bits[1] * Draw.scl, region.height * bits[2] * Draw.scl, req.rotation * 90);
            Draw.color();
            region = topRegions[bits[0]];
            Draw.rect(region, req.drawx(), req.drawy(), region.width * bits[1] * Draw.scl, region.height * bits[2] * Draw.scl, req.rotation * 90);
        }
    }

    public boolean checkDeep(float x, float y){
        Tile t = world.tileWorld(x, y);
        if(t != null && t.floor().isLiquid){
            if(drawShallow || t.floor().isDeep()){
                return true;
            }
        }
        return false;
    }

    public class FloatingConveyorBuild extends ConveyorBuild{
        public boolean deep;

        @Override
        public void placed(){
            super.placed();

            deep = checkDeep(x, y);
        }

        @Override
        public void draw(){
            int frame = enabled && clogHeat <= 0.5f ? (int)(((Time.time * (deep ? deepSpeed : speed) * 8f * timeScale)) % 4) : 0;

            //draw extra conveyors facing this one for non-square tiling purposes
            Draw.z(Layer.blockUnder);
            for(int i = 0; i < 4; i++){
                if((blending & (1 << i)) != 0){
                    int dir = rotation - i;
                    float rot = i == 0 ? rotation * 90 : (dir)*90;

                    Draw.rect(sliced(regions[0][frame], i != 0 ? SliceMode.bottom : SliceMode.top), x + Geometry.d4x(dir) * tilesize*0.75f, y + Geometry.d4y(dir) * tilesize*0.75f, rot);
                }
            }

            Draw.z(Layer.block - 0.2f);

            Draw.rect(regions[blendbits][frame], x, y, tilesize * blendsclx, tilesize * blendscly, rotation * 90);

            Draw.z(Layer.block - 0.1f);

            for(int i = 0; i < len; i++){
                Item item = ids[i];
                tr1.trns(rotation * 90, tilesize, 0);
                tr2.trns(rotation * 90, -tilesize / 2f, xs[i] * tilesize / 2f);

                Draw.rect(item.icon(Cicon.medium),
                    (tile.x * tilesize + tr1.x * ys[i] + tr2.x),
                    (tile.y * tilesize + tr1.y * ys[i] + tr2.y),
                    itemSize, itemSize);
            }
            
            if(deep){
                Draw.z(Layer.block - 0.09f);
                Draw.color(world.tileWorld(x, y).floor().mapColor, coverOpacity);
                Draw.rect(coverRegions[blendbits], x, y, coverRegions[blendbits].width / 4f * blendsclx, coverRegions[blendbits].height / 4f * blendscly, rotation * 90);
                Draw.color();

                Draw.z(Layer.block - 0.08f);
                Draw.rect(topRegions[blendbits], x, y, topRegions[blendbits].width / 4f * blendsclx, topRegions[blendbits].height / 4f * blendscly, rotation * 90);
            }
        }

        @Override
        public void unitOn(Unit unit){
            if(!deep){
                super.unitOn(unit);
            }
        }

        @Override
        public void updateTile(){
            minitem = 1f;
            mid = 0;

            //skip updates if possible
            if(len == 0){
                clogHeat = 0f;
                sleep();
                return;
            }

            float nextMax = aligned ? 1f - Math.max(itemSpace - nextc.minitem, 0) : 1f;
            float moved = (deep ? deepSpeed : speed) * edelta();

            for(int i = len - 1; i >= 0; i--){
                float nextpos = (i == len - 1 ? 100f : ys[i + 1]) - itemSpace;
                float maxmove = Mathf.clamp(nextpos - ys[i], 0, moved);

                ys[i] += maxmove;

                if(ys[i] > nextMax) ys[i] = nextMax;
                if(ys[i] > 0.5 && i > 0) mid = i - 1;
                xs[i] = Mathf.approachDelta(xs[i], 0, speed*2);

                if(ys[i] >= 1f && pass(ids[i])){
                    //align X position if passing forwards
                    if(aligned){
                        nextc.xs[nextc.lastInserted] = xs[i];
                    }
                    //remove last item
                    items.remove(ids[i], len - i);
                    len = Math.min(i, len);
                }else if(ys[i] < minitem){
                    minitem = ys[i];
                }
            }

            if(minitem < itemSpace + (blendbits == 1 ? 0.3f : 0f)){
                clogHeat = Mathf.lerpDelta(clogHeat, 1f, 0.02f);
            }else{
                clogHeat = 0f;
            }

            noSleep();
        }
    }
}