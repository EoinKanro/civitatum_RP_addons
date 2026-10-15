package ru.teacraft.civitatum.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.teacraft.civitatum.*;
import ru.teacraft.civitatum.client.*;

/** Development-only real-client screenshots and save roundtrip. */
@Mod(value="civitatum_smoke",dist=Dist.CLIENT)
public final class SmokeClient {
    private int phase,ticks;
    public SmokeClient(){NeoForge.EVENT_BUS.addListener(this::tick);}
    private void tick(ClientTickEvent.Post e) {
        var game=Minecraft.getInstance();ticks++;
        if(ticks%100==0) System.out.println("CIVITATUM_SCREEN_PHASE "+phase+" "+(game.screen==null?"world":game.screen.getClass().getName()));
        if(game.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            game.options.onboardAccessibility=false;game.options.save();game.setScreen(new TitleScreen());
        }
        if(phase==0 && game.screen instanceof TitleScreen && ticks>80) {
            phase=1;ticks=0;CreateWorldScreen.openFresh(game,game.screen);
        } else if(phase==1 && game.screen instanceof CreateWorldScreen screen && ticks>40) {
            phase=2;ticks=0;
            try {var method=CreateWorldScreen.class.getDeclaredMethod("onCreate");method.setAccessible(true);method.invoke(screen);}
            catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        } else if(phase==2 && game.player!=null && game.level!=null && game.screen==null) {
            phase=3;ticks=0;game.options.guiScale().set(2);game.resizeDisplay();
            game.getSingleplayerServer().execute(()->{
                var p=game.getSingleplayerServer().getPlayerList().getPlayer(game.player.getUUID());RaceService.open(p,true,false);
            });
        } else if(phase==3 && game.screen instanceof CharacterScreen screen && ticks>100) {
            capture(game,"01-race-and-sex.png");screen.selectForTest("wood_elf","female");screen.stageForTest(1);phase=4;ticks=0;
        } else if(phase==4 && game.screen instanceof CharacterScreen screen && ticks>45) {
            capture(game,"02-subrace.png");screen.stageForTest(2);phase=5;ticks=0;
        } else if(phase==5 && game.screen instanceof CharacterScreen screen && ticks>45) {
            capture(game,"03-appearance-template.png");click(screen,500,175);phase=50;ticks=0;
        } else if(phase==50 && game.screen instanceof CharacterScreen screen && ticks>45) {
            if(!screen.draft().usePlayerSkin)throw new IllegalStateException("Own-skin button failed");
            capture(game,"03b-own-skin.png");click(screen,650,175);
            screen.selectForTest("orthosian","male");screen.draft().tattoo=6;phase=6;ticks=0;
        } else if(phase==6 && game.screen instanceof CharacterScreen screen && ticks>45) {
            capture(game,"04-orthosian.png");screen.selectForTest("forest_algnor","female");screen.draft().horns=5;screen.draft().tail=1;phase=7;ticks=0;
        } else if(phase==7 && game.screen instanceof CharacterScreen screen && ticks>45) {
            capture(game,"05-algnor.png");screen.selectForTest("human","female");screen.draft().name="Арина";screen.stageForTest(3);phase=8;ticks=0;
        } else if(phase==8 && game.screen instanceof CharacterScreen screen && ticks>45) {
            capture(game,"06-confirmation.png");
            PacketDistributor.sendToServer(new RaceNetwork.Serverbound(0,screen.tokenForTest(),RaceCatalog.validate(screen.draft()).json()));
            phase=9;ticks=0;
        } else if(phase==9 && game.player!=null && ticks>80) {
            var c=ClientCharacters.get(game.player);
            if(!c.selected || !c.race.equals("human") || !c.female() || !c.name.equals("Арина")) throw new IllegalStateException("Save/sync mismatch");
            System.out.println("CIVITATUM_CLIENT_SAVE_PASS");
            if(net.neoforged.fml.ModList.get().isLoaded("wildfire_gender") && !FemaleGenderBridge.available())
                throw new IllegalStateException("Female Gender Mod adapter failed");
            System.out.println("CIVITATUM_FEMALE_ADAPTER "+FemaleGenderBridge.available());
            if(FemaleGenderBridge.available())try {
                Object config=Class.forName("com.wildfire.main.WildfireGender").getMethod("getOrAddPlayerById",java.util.UUID.class).invoke(null,game.player.getUUID());
                if(!config.getClass().getMethod("getGender").invoke(config).toString().equals("FEMALE"))throw new IllegalStateException("Gender wasn't applied");
                if(Math.abs(((Number)config.getClass().getMethod("getBustSize").invoke(config)).floatValue()-c.bust)>.001)throw new IllegalStateException("Bust wasn't applied");
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            PacketDistributor.sendToServer(new RaceNetwork.Serverbound(1,0,""));
            game.options.guiScale().set(3);game.resizeDisplay();
            game.getSingleplayerServer().execute(()->RaceService.open(game.getSingleplayerServer().getPlayerList().getPlayer(game.player.getUUID()),false,false));
            phase=10;ticks=0;
        } else if(phase==10 && game.screen instanceof CharacterScreen && ticks>60) {
            if(ClientCharacters.state(game.player).cooldownUntil()<=game.level.getGameTime())throw new IllegalStateException("Ability packet wasn't synchronized");
            capture(game,"07-compact-editor.png");
            PacketDistributor.sendToServer(new RaceNetwork.Serverbound(0,0,RaceCatalog.defaults("orc","male").json()));
            phase=11;ticks=0;
        } else if(phase==11 && ticks>60) {
            if(!ClientCharacters.get(game.player).race.equals("human")) throw new IllegalStateException("Unauthorized race change was accepted");
            System.out.println("CIVITATUM_CLIENT_UNAUTHORIZED_SAVE_REJECTED");
            click((CharacterScreen)game.screen,710,115);phase=12;ticks=0;
        } else if(phase==12 && game.screen instanceof CharacterScreen screen && ticks>45) {
            capture(game,"08-body-settings.png");click(screen,450,375);phase=13;ticks=0;
        } else if(phase==13 && game.screen instanceof CharacterScreen screen && ticks>45) {
            capture(game,"09-body-physics.png");
            game.getSingleplayerServer().execute(()->{
                var p=game.getSingleplayerServer().getPlayerList().getPlayer(game.player.getUUID());
                p.getInventory().add(new net.minecraft.world.item.ItemStack(Civitatum.SHARD.get()));RaceService.openWithShard(p);
            });phase=14;ticks=0;
        } else if(phase==14 && game.screen instanceof CharacterScreen screen && ticks>60) {
            screen.draft().hair=1;
            PacketDistributor.sendToServer(new RaceNetwork.Serverbound(0,screen.tokenForTest(),RaceCatalog.validate(screen.draft()).json()));
            phase=15;ticks=0;
        } else if(phase==15 && ticks>80) {
            if(ClientCharacters.get(game.player).hair!=1)throw new IllegalStateException("Shard editor didn't save");
            game.getSingleplayerServer().execute(()->{
                var p=game.getSingleplayerServer().getPlayerList().getPlayer(game.player.getUUID());
                if(p.getInventory().contains(new net.minecraft.world.item.ItemStack(Civitatum.SHARD.get())))throw new IllegalStateException("Shard wasn't consumed");
                RaceService.reset(p);
            });phase=16;ticks=0;
        } else if(phase==16 && ticks>60) {
            if(ClientCharacters.get(game.player).selected)throw new IllegalStateException("Reset didn't synchronize");
            System.out.println("CIVITATUM_CLIENT_SHARD_AND_RESET_PASS");
            game.stop();
        }
    }
    private void click(CharacterScreen screen,int x,int y) {
        float zoom=Math.min(screen.width/800f,screen.height/450f);
        screen.mouseClicked((screen.width-800*zoom)/2+x*zoom,(screen.height-450*zoom)/2+y*zoom,0);
    }
    private void capture(Minecraft game,String name) {
        var directory=new java.io.File("../build/client-captures"+(net.neoforged.fml.ModList.get().isLoaded("wildfire_gender")?"-female":""));directory.mkdirs();
        net.minecraft.client.Screenshot.grab(directory,name,game.getMainRenderTarget(),message->System.out.println("CIVITATUM_CAPTURE "+message.getString()));
    }
}
