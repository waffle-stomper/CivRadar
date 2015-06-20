package com.biggestnerd.civradar;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

import com.biggestnerd.civradar.Config.NameLocation;

public class RenderHandler extends Gui {

	private Config config = CivRadar.instance.getConfig();
	private Minecraft mc = Minecraft.getMinecraft();
	private Color radarColor;
	private double pingDelay = 63.0D;
	private List entityList;
	private float radarScale;
	ArrayList<String> inRangePlayers;
	private Color dubstepColor = Color.BLACK;
	private long dubstepTimer = 0;
	
	public RenderHandler() {
		inRangePlayers = new ArrayList<String>();
	}
	
	@SubscribeEvent
	public void renderRadar(RenderGameOverlayEvent event) {
		if(event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS)
			return;
		if(config.isDubstepMode()) {
			GLUtils.glPushMatrix();
			GLUtils.glScalef(2.0f, 2.0f, 2.0f);
			ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
			int halfWidth = res.getScaledWidth() / 4;
			int stringWidth = mc.fontRendererObj.getStringWidth("Dubstep Mode Enabled");
			int height = res.getScaledHeight() / 8;
			mc.fontRendererObj.drawStringWithShadow("Dubstep Mode Enabled", halfWidth - (stringWidth / 2), height, dubstepColor.getRGB());
			GLUtils.glScalef(1.0f, 1.0f, 1.0f);
			GLUtils.glPopMatrix();
		}
		if(config.isEnabled()) {
			drawRadar();
		}
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent event) {
		if(event.phase == TickEvent.Phase.START && mc.theWorld != null) {
			if(pingDelay <= -10.0D) {
				pingDelay = 63.0D;
			}
			pingDelay -= 1.0D;
			entityList = mc.theWorld.loadedEntityList;
			ArrayList<String> newInRangePlayers = new ArrayList();
			for(Object o : entityList) {
				if(o instanceof EntityOtherPlayerMP) {
					newInRangePlayers.add(((EntityOtherPlayerMP)o).getName());
				}
			}
			ArrayList<String> temp = (ArrayList)newInRangePlayers.clone();
			newInRangePlayers.removeAll(inRangePlayers);
			for(String name : newInRangePlayers) {	
				mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "minecraft:note.pling", config.getPingVolume(), 1.0F, false);
			}
			inRangePlayers = temp;
			
			if(config.isDubstepMode()) {
				Random rand = new Random();
				dubstepColor = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
				if((System.currentTimeMillis() - 202000) >= dubstepTimer) {
					dubstepTimer = System.currentTimeMillis();
					mc.theWorld.playSoundAtEntity(mc.thePlayer, "civradar:dubstep", 1, 1);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void renderWaypoints(RenderWorldLastEvent event) {
		if(CivRadar.instance.getWaypointSave() == null) {
			return;
		}
		if(config.isRenderWaypoints()) {
			for(Waypoint point : CivRadar.instance.getWaypointSave().getWaypoints()) {
				if(point.getDimension() == mc.theWorld.provider.getDimensionId() && point.isEnabled()) {
					renderWaypoint(point, event);
				}
			}
		}
	}
	
	private void drawRadar() {
		radarColor = config.getRadarColor();
		if(config.isDubstepMode()) {
			radarColor = dubstepColor;
		}
		radarScale = config.getRadarScale();
		ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
		int width = res.getScaledWidth();
		GLUtils.glPushMatrix();
		GLUtils.glTranslatef(width - (65 * radarScale) + (config.getRadarX()), (65 * radarScale) + (config.getRadarY()), 0.0F);
		GLUtils.glScalef(1.0F, 1.0F, 1.0F);
		if(config.isRenderCoordinates()) {
			String coords = "(" + (int) mc.thePlayer.posX + "," + (int) mc.thePlayer.posY + "," + (int) mc.thePlayer.posZ + ")";
			mc.fontRendererObj.drawStringWithShadow(coords, -(mc.fontRendererObj.getStringWidth(coords) / 2), 65 * radarScale, 14737632);
		}
		GLUtils.glScalef(radarScale, radarScale, radarScale);
		GLUtils.glRotatef(-mc.thePlayer.rotationYaw, 0.0F, 0.0F, 1.0F);
		drawCircle(0, 0, 63.0D, radarColor, true);
		GLUtils.glLineWidth(2.0F);
		drawCircle(0, 0, 63.0D, radarColor, false);
		GLUtils.glLineWidth(1.0F);
		
		if(pingDelay > 0) {
			drawCircle(0, 0, 63.0D - pingDelay, radarColor, false);
		}
		GLUtils.glLineWidth(2.0F);
		GLUtils.glDisable(GLUtils.GL_TEXTURE_2D);
		GLUtils.glDisable(GLUtils.GL_LIGHTING);
		GLUtils.glBegin(1);
		GLUtils.glColor4f(radarColor.getRed() / 255.0F, radarColor.getGreen() / 255.0F, radarColor.getBlue() / 255.0F, config.getRadarOpacity() + 0.5F);
		GLUtils.glVertex2d(0.0D, -63.0D);
		GLUtils.glVertex2d(0.0D, 63.0D);
		GLUtils.glVertex2d(-63.0D, 0.0D);
		GLUtils.glVertex2d(63.0D, 0.0D);
		GLUtils.glVertex2d(-44.5D, -44.5D);
		GLUtils.glVertex2d(44.5D, 44.5D);
		GLUtils.glVertex2d(-44.5D, 44.5D);
		GLUtils.glVertex2d(44.5D, -44.5D);
		GLUtils.glEnd();
		GLUtils.glDisable(GLUtils.GL_BLEND);
		GLUtils.glEnable(GLUtils.GL_TEXTURE_2D);
		
		drawRadarIcons();
		
		GLUtils.glRotatef(mc.thePlayer.rotationYaw, 0.0F, 0.0F, 1.0F);
		
		drawTriangle(0, 0, Color.WHITE);
		GLUtils.glScalef(2.0F, 2.0F, 2.0F);
		GLUtils.glPopMatrix();
	}
	
	private void drawCircle(int x, int y, double radius, Color c, boolean filled) {
		GLUtils.glEnable(3042);
		GLUtils.glDisable(GLUtils.GL_TEXTURE_2D);
		GLUtils.glEnable(2848);
		GLUtils.glBlendFunc(770, 771);
		GLUtils.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, filled ? config.getRadarOpacity() : config.getRadarOpacity() + 0.5F);
		GLUtils.glBegin(filled ? 6 : 2);
		for (int i = 0; i <= 360; i++) {
			double x2 = Math.sin(i * Math.PI / 180.0D) * radius;
			double y2 = Math.cos(i * Math.PI / 180.0D) * radius;
			GLUtils.glVertex2d(x + x2, y + y2);
		}
		GLUtils.glEnd();
		GLUtils.glDisable(2848);
		GLUtils.glEnable(GLUtils.GL_TEXTURE_2D);
		GLUtils.glDisable(3042);
	}
	
	private void drawTriangle(int x, int y, Color c) {
		GLUtils.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
		GLUtils.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
		GLUtils.glEnable(3042);
		GLUtils.glDisable(3553);
		GLUtils.glEnable(2848);
		GLUtils.glBlendFunc(770, 771);
		GLUtils.glBegin(4);
		GLUtils.glVertex2d(x, y + 3);
		GLUtils.glVertex2d(x + 3, y - 3);
		GLUtils.glVertex2d(x - 3, y - 3);
		GLUtils.glEnd();
		GLUtils.glDisable(2848);
		GLUtils.glEnable(3553);
		GLUtils.glDisable(3042);
		GLUtils.glRotatef(-180.0F, 0.0F, 0.0F, 1.0F);
	}
	
	private void drawRadarIcons() {
		if(entityList == null) {
			return;
		}
		for(Object o : entityList) {
			Entity e = (Entity) o;
			int playerPosX = (int) mc.thePlayer.posX;
			int playerPosZ = (int) mc.thePlayer.posZ;
			int entityPosX = (int) e.posX;
			int entityPosZ = (int) e.posZ;
			int displayPosX = playerPosX - entityPosX;
			int displayPosZ = playerPosZ - entityPosZ;
			if(e != mc.thePlayer) {
				if(e instanceof EntityItem) {
					EntityItem item = (EntityItem) e;
					if(config.isRender(EntityItem.class)) {
						renderItemIcon(displayPosX, displayPosZ, item.getEntityItem());
					}
				} else if(e instanceof EntityOtherPlayerMP) {
					if(config.isRender(EntityPlayer.class)) {
						EntityOtherPlayerMP eop = (EntityOtherPlayerMP) e;
						try {
							renderPlayerHeadIcon(displayPosX, displayPosZ, eop);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				} else if(e instanceof EntityMinecart) {
					if(config.isRender(EntityMinecart.class)) {
						ItemStack cart = new ItemStack(Items.minecart);
						renderItemIcon(displayPosX, displayPosZ, cart);
					}
				} else if(config.isRender(o.getClass())) {
					renderIcon(displayPosX, displayPosZ, config.getMob(o.getClass()).getResource());
				}
			}
		}
	}
	
	private void renderItemIcon(int x, int y, ItemStack item) {
		GLUtils.glPushMatrix();
		GLUtils.glScalef(0.5F, 0.5F, 0.5F);
		GLUtils.glTranslatef(x +1, y +1, 0.0F);
		GLUtils.glColor4f(1.0F, 1.0F, 1.0F, config.getRadarOpacity() + 0.5F);
		GLUtils.glRotatef(mc.thePlayer.rotationYaw, 0.0F, 0.0F, 1.0F);
		mc.getRenderItem().renderItemIntoGUI(item, -8, -8);
		GLUtils.glTranslatef(-x -1, -y -1, 0.0F);
		GLUtils.glScalef(2.0F, 2.0F, 2.0F);
		GLUtils.glDisable(2896);
		GLUtils.glPopMatrix();
	}
	
	private void renderPlayerHeadIcon(int x, int y, EntityOtherPlayerMP player) throws Exception {
		GLUtils.glColor4f(1.0F, 1.0F, 1.0F, config.getRadarOpacity() + 0.5F);
		GLUtils.glEnable(3042);
		GLUtils.glPushMatrix();
		GLUtils.glScalef(0.5F, 0.5F, 0.5F);
		GLUtils.glTranslatef(x + 1, y + 1, 0.0F);
		GLUtils.glRotatef(mc.thePlayer.rotationYaw, 0.0F, 0.0F, 1.0F);
		mc.getTextureManager().bindTexture(new ResourceLocation("civRadar/icons/player.png"));
		drawModalRectWithCustomSizedTexture(-8, -8, 0, 0, 16, 16, 16, 16);
		GLUtils.glTranslatef(-x -1, -y -1, 0.0F);
		GLUtils.glScalef(2.0F, 2.0F, 2.0F);
		GLUtils.glDisable(2896);
		GLUtils.glDisable(3042);
		GLUtils.glPopMatrix();
		if(config.isPlayerNames()) {
			GLUtils.glPushMatrix();
			GLUtils.glScalef(0.5F, 0.5F, 0.5F);
			GLUtils.glTranslatef(x, y, 0.0F);
			GLUtils.glRotatef(mc.thePlayer.rotationYaw, 0.0F, 0.0F, 1.0F);
			GLUtils.glTranslatef(-x - 8, -y, 0.0F);
			String playerName = player.getName();
			if(config.isExtraPlayerInfo()) {
				playerName += " (" + (int) mc.thePlayer.getDistanceToEntity(player) + "m)(Y" + (int) player.posY + ")";
			}
			int yOffset = config.getNameLocation() == NameLocation.below ? 10 : -10;
			drawCenteredString(mc.fontRendererObj, playerName, x + 8, y + yOffset, Color.WHITE.getRGB());
			GLUtils.glScalef(2.0F, 2.0F, 2.0F);
			GLUtils.glPopMatrix();
		}
	}
	
	private void renderIcon(int x, int y, ResourceLocation resource) {
		mc.getTextureManager().bindTexture(resource);
		GLUtils.glColor4f(1.0F, 1.0F, 1.0F, config.getRadarOpacity() + 0.5F);
		GLUtils.glEnable(3042);
		GLUtils.glPushMatrix();
		GLUtils.glScalef(0.5F, 0.5F, 0.5F);
		GLUtils.glTranslatef(x + 1, y + 1, 0.0F);
		GLUtils.glRotatef(mc.thePlayer.rotationYaw, 0.0F, 0.0F, 1.0F);
		drawModalRectWithCustomSizedTexture(-8, -8, 0, 0, 16, 16, 16, 16);
		GLUtils.glTranslatef(-x -1, -y -1, 0.0F);
		GLUtils.glScalef(2.0F, 2.0F, 2.0F);
		GLUtils.glDisable(2896);
		GLUtils.glDisable(3042);
		GLUtils.glPopMatrix();
	}
	
	private void renderWaypoint(Waypoint point, RenderWorldLastEvent event) {
		String name = point.getName();
		Color c = point.getColor();
		float partialTickTime = event.partialTicks;
		double distance = point.getDistance(mc);
		if(distance <= config.getMaxWaypointDistance() || config.getMaxWaypointDistance() < 0) {
			FontRenderer fr = mc.fontRendererObj;
			Tessellator tess = Tessellator.getInstance();
			WorldRenderer wr = tess.getWorldRenderer();
			RenderManager rm = mc.getRenderManager();
			
			float playerX = (float) (mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTickTime);
			float playerY = (float) (mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTickTime);
			float playerZ = (float) (mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTickTime);
			
			float displayX = (float)point.getX() - playerX;
			float displayY = (float)point.getY() + 1.3f - playerY;
			float displayZ = (float)point.getZ() - playerZ;
			
			float scale = (float) (Math.max(2, distance /5) * 0.0185f);
			
			GLUtils.glColor4f(1f, 1f, 1f, 1f);
			GLUtils.glPushMatrix();
			GLUtils.glTranslatef(displayX, displayY, displayZ);
			GLUtils.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
			GLUtils.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
			GLUtils.glScalef(-scale, -scale, scale);
			GLUtils.glDisable(GLUtils.GL_LIGHTING);
			GLUtils.glDepthMask(false);
			GLUtils.glDisable(GLUtils.GL_DEPTH_TEST);
			GLUtils.glEnable(GLUtils.GL_BLEND);
			GLUtils.glBlendFunc(GLUtils.GL_SRC_ALPHA, GLUtils.GL_ONE_MINUS_SRC_ALPHA);
			
			name += " (" + (int)distance + "m)";
			int width = fr.getStringWidth(name);
			int height = 10;
			GLUtils.glDisable(GLUtils.GL_TEXTURE_2D);
			wr.startDrawingQuads();
			int stringMiddle = width / 2;
			wr.setColorRGBA_F(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, config.getWaypointOpcaity());
			wr.addVertex(-stringMiddle - 1, -1, 0.0D);
			wr.addVertex(-stringMiddle - 1, 1 + height, 0.0D);
			wr.addVertex(stringMiddle + 1, 1 + height, 0.0D);
			wr.addVertex(stringMiddle + 1,  -1, 0.0D);
			tess.draw();
			GLUtils.glEnable(GLUtils.GL_TEXTURE_2D);
			
			fr.drawString(name, -width / 2, 1, Color.WHITE.getRGB());
			GLUtils.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GLUtils.glDepthMask(true);
			GLUtils.glEnable(GLUtils.GL_DEPTH_TEST);
			GLUtils.glPopMatrix();
		}
	}
}
