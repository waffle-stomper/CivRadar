package com.biggestnerd.civradar;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.lwjgl.input.Keyboard;

import com.biggestnerd.civradar.gui.GuiRadarOptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

@Mod(modid=CivRadar.MODID, name=CivRadar.MODNAME, version=CivRadar.VERSION)
public class CivRadar {
	public final static String MODID = "civradar";
	public final static String MODNAME = "CivRadar";
	public final static String VERSION = "1.1.0";
	private RenderHandler renderHandler;
	private Config radarConfig;
	private File configFile;
	private KeyBinding radarOptions = new KeyBinding("CivRadar Settings", Keyboard.KEY_R, "CivRadar");
	Minecraft mc;
	public static CivRadar instance;
	private File saveFile;
	public static File waypointDir;
	private File radarDir;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		mc = Minecraft.getMinecraft();
		instance = this;
		File oldConfig = new File(event.getModConfigurationDirectory(), "civRadar.json");
		File radarDir = new File(mc.mcDataDir, "/civradar/");
		if(!radarDir.isDirectory()) {
			radarDir.mkdir();
		}
		configFile = new File(radarDir, "config.json");
		if(oldConfig.exists()) {
			try {
				FileUtils.copyFile(oldConfig, configFile);
				oldConfig.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(!configFile.isFile()) {
			try {
				configFile.createNewFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
			radarConfig = new Config();
			radarConfig.save(configFile);
		} else {
			radarConfig = Config.load(configFile);
			if(radarConfig == null) {
				radarConfig = new Config();
			}
			radarConfig.save(configFile);
		}
		renderHandler = new RenderHandler();
		
		waypointDir = new File(radarDir, "/waypoints/");
		if(!waypointDir.isDirectory()) {
			waypointDir.mkdir();
		}
		FMLCommonHandler.instance().bus().register(renderHandler);
		MinecraftForge.EVENT_BUS.register(renderHandler);
		FMLCommonHandler.instance().bus().register(this);
		ClientRegistry.registerKeyBinding(radarOptions);
	}
	
	@SubscribeEvent
	public void keyPress(KeyInputEvent event) {
		if(radarOptions.isPressed()) {
			mc.displayGuiScreen(new GuiRadarOptions(mc.currentScreen));
		}
	}
	
	public Config getConfig() {
		return radarConfig;
	}
	
	public void saveConfig() {
		radarConfig.save(configFile);
	}
}
