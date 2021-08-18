package progressed.world.blocks.payloads;

import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Missile extends NuclearWarhead{
    public Block prev;
    public float powerUse, constructTime = -1;
    public boolean requiresUnlock;

    public float shadowRad = -1f;

    public Missile(String name){
        super(name);

        buildVisibility = BuildVisibility.sandboxOnly;
        category = Category.units;
        researchCostMultiplier = 5f;
        hasShadow = false;
        rebuildable = false;
    }

    @Override
    public void init(){
        if(constructTime < 0) constructTime = buildCost;
        if(shadowRad < 0) shadowRad = size * tilesize * 1.5f;

        super.init();
    }

    public void drawBase(Tile tile){
        Drawf.shadow(tile.drawx(), tile.drawy(), shadowRad);
        Draw.rect(region, tile.drawx(), tile.drawy());
    }
}