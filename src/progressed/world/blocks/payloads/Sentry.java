package progressed.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

public class Sentry extends Missile{
    public UnitType unit;

    public Sentry(String name){
        super(name);
        rotate = true;
        configurable = true;
        breakSound = destroySound = Sounds.none;
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy(), req.rotation * 90 - 90f);
    }

    @Override
    public void drawBase(Tile tile){
        Drawf.shadow(tile.drawx(), tile.drawy(), shadowRad);
        Draw.rect(region, tile.drawx(), tile.drawy(), tile.build.rotdeg() - 90f);
    }

    public class SentryBuild extends Building{
        @Override
        public void buildConfiguration(Table table){
            Table cont = new Table();
            cont.defaults().size(40);

            ImageButton button = cont.button(Tex.whiteui, Styles.clearToggleTransi, 24, this::spawn).get();
            button.getStyle().imageUp = Icon.upload;

            table.add(cont);
        }

        public void spawn(){
            Unit spawned = unit.spawn(team, self());
            spawned.rotation(rotdeg());
            kill();
        }

        @Override
        public void onDestroyed(){
            //no
        }
    }
}