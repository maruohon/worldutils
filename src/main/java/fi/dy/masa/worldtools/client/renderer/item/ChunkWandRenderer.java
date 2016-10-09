package fi.dy.masa.worldtools.client.renderer.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import fi.dy.masa.worldtools.item.ItemChunkWand;
import fi.dy.masa.worldtools.item.ItemChunkWand.Corner;
import fi.dy.masa.worldtools.item.ItemChunkWand.Mode;
import fi.dy.masa.worldtools.setup.WorldToolsItems;

public class ChunkWandRenderer
{
    protected final Minecraft mc;
    protected final List<ChunkPos> positionsStored;
    protected float partialTicksLast;

    public ChunkWandRenderer()
    {
        this.mc = Minecraft.getMinecraft();
        this.positionsStored = new ArrayList<ChunkPos>();
    }

    public void renderSelectedArea(World world, EntityPlayer usingPlayer, ItemStack stack, EntityPlayer clientPlayer, float partialTicks)
    {
        ItemChunkWand wand = (ItemChunkWand) stack.getItem();
        Mode mode = Mode.getMode(stack);

        if (partialTicks < this.partialTicksLast)
        {
            this.positionsStored.clear();
            this.positionsStored.addAll(wand.getStoredSelection(stack));
        }

        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();

        ChunkPos posStart = wand.getPosition(stack, Corner.START);
        ChunkPos posEnd = wand.getPosition(stack, Corner.END);

        this.renderChunkOutlines(this.positionsStored, clientPlayer, posStart, posEnd, partialTicks, 0.3f, 1.0f, 0.3f);

        this.renderStartAndEndPositions(mode, clientPlayer, posStart, posEnd, partialTicks);

        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);

        this.partialTicksLast = partialTicks;
    }

    private void renderChunkOutlines(List<ChunkPos> positions, EntityPlayer player, ChunkPos posStart, ChunkPos posEnd,
            float partialTicks, float red, float green, float blue)
    {
        GlStateManager.glLineWidth(2.0f);
        float expand = 0f;

        for (int i = 0; i < positions.size(); i++)
        {
            ChunkPos pos = positions.get(i);

            if (pos.equals(posStart) == false && pos.equals(posEnd) == false)
            {
                AxisAlignedBB aabb = this.createChunkAABB(pos, expand, partialTicks, player);
                RenderGlobal.drawSelectionBoundingBox(aabb, red, green, blue, 1.0f);
            }
        }
    }

    private void renderStartAndEndPositions(Mode mode, EntityPlayer player, ChunkPos posStart, ChunkPos posEnd, float partialTicks)
    {
        this.renderStartAndEndPositions(mode, player, posStart, posEnd, partialTicks, 0xFF, 0xFF, 0xFF);
    }

    private void renderStartAndEndPositions(Mode mode, EntityPlayer player, ChunkPos posStart, ChunkPos posEnd, float partialTicks, int r, int g, int b)
    {
        // Draw the area bounding box
        if (posStart != null && posEnd != null)
        {
            AxisAlignedBB aabb = this.createEnclosingChunkAABB(posStart, posEnd, player, partialTicks);
            RenderGlobal.drawSelectionBoundingBox(aabb, r / 255f, g / 255f, b / 255f, 0xCC / 255f);
        }

        float expand = 0f;

        if (posStart != null)
        {
            // Render the targeted position in a different (hilighted) color
            GlStateManager.glLineWidth(3.0f);
            AxisAlignedBB aabb = this.createChunkAABB(posStart, expand, partialTicks, player);
            RenderGlobal.drawSelectionBoundingBox(aabb, 1.0f, 0x33 / 255f, 0x33 / 255f, 1.0f);
        }

        if (posEnd != null)
        {
            // Render the end position in a different (hilighted) color
            GlStateManager.glLineWidth(3.0f);
            AxisAlignedBB aabb = this.createChunkAABB(posEnd, expand, partialTicks, player);
            RenderGlobal.drawSelectionBoundingBox(aabb, 0x33 / 255f, 0x33 / 255f, 1.0f, 1.0f);
        }
    }

    public static void renderText(Minecraft mc, int posX, int posY, List<String> lines)
    {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int lineHeight = mc.fontRendererObj.FONT_HEIGHT + 2;
        int y = scaledResolution.getScaledHeight() - (lineHeight * lines.size());

        y = y + posY;
        boolean useTextBackground = true;
        boolean useFontShadow = true;
        int textBgColor = 0x80000000;
        FontRenderer fontRenderer = mc.fontRendererObj;

        for (String line : lines)
        {
            if (useTextBackground)
            {
                Gui.drawRect(posX - 2, y - 2, posX + fontRenderer.getStringWidth(line) + 2, y + fontRenderer.FONT_HEIGHT, textBgColor);
            }

            if (useFontShadow)
            {
                mc.ingameGUI.drawString(fontRenderer, line, posX, y, 0xFFFFFFFF);
            }
            else
            {
                fontRenderer.drawString(line, posX, y, 0xFFFFFFFF);
            }

            y += fontRenderer.FONT_HEIGHT + 2;
        }
    }

    public void renderHudChunkWand(EntityPlayer player)
    {
        ItemStack stack = this.mc.thePlayer.getHeldItemMainhand();
        if (stack == null || stack.getItem() != WorldToolsItems.chunkWand)
        {
            return;
        }

        List<String> lines = new ArrayList<String>();

        this.getText(lines, stack, player);

        renderText(this.mc, 4, 0, lines);
    }

    private void getText(List<String> lines, ItemStack stack, EntityPlayer player)
    {
        ItemChunkWand wand = (ItemChunkWand) stack.getItem();
        Mode mode = Mode.getMode(stack);
        String preGreen = TextFormatting.GREEN.toString();
        String rst = TextFormatting.RESET.toString() + TextFormatting.WHITE.toString();
        String preIta = TextFormatting.ITALIC.toString();
        int index = wand.getTargetSelection(stack);
        int max = wand.getNumTargets(stack);

        String str = I18n.format("enderutilities.tooltip.item.chunkwand.target");
        String name = wand.getWorldName(stack);
        lines.add(String.format("%s [%s%d/%d%s]: %s%s%s", str, preGreen, (index + 1), max, rst, preIta, name, rst));

        String modeName = mode.getDisplayName();
        str = I18n.format("enderutilities.tooltip.item.mode");
        String strMode = String.format("%s [%s%d/%d%s]: %s%s%s", str, preGreen,
                (mode.ordinal() + 1), Mode.getModeCount(player), rst, preGreen, modeName, rst);

        lines.add(strMode);
    }

    private int getMaxY(EntityPlayer player)
    {
        return MathHelper.clamp_int(Math.min((int) player.posY - 16, 120), 1, 255);
    }

    private AxisAlignedBB createEnclosingChunkAABB(ChunkPos posStart, ChunkPos posEnd, EntityPlayer player, float partialTicks)
    {
        int minX = Math.min(posStart.chunkXPos, posEnd.chunkXPos);
        int minZ = Math.min(posStart.chunkZPos, posEnd.chunkZPos);
        int maxX = Math.max(posStart.chunkXPos, posEnd.chunkXPos) + 1;
        int maxZ = Math.max(posStart.chunkZPos, posEnd.chunkZPos) + 1;
        int yMax = this.getMaxY(player);

        return this.createChunkAABB(minX, 0, minZ, maxX, yMax, maxZ, 0, partialTicks, player);
    }

    private AxisAlignedBB createChunkAABB(ChunkPos pos, double expand, double partialTicks, EntityPlayer player)
    {
        return this.createChunkAABB(pos.chunkXPos, pos.chunkZPos, expand, partialTicks, player);
    }

    private AxisAlignedBB createChunkAABB(int chunkX, int chunkZ, double expand, double partialTicks, EntityPlayer player)
    {
        int yMax = this.getMaxY(player);
        return this.createChunkAABB(chunkX, 0, chunkZ, chunkX + 1, yMax, chunkZ + 1, expand, partialTicks, player);
    }

    private AxisAlignedBB createChunkAABB(int minChunkX, int minY, int minChunkZ, int maxChunkX, int maxY, int maxChunkZ,
            double expand, double partialTicks, EntityPlayer player)
    {
        double dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        return new AxisAlignedBB(   minChunkX * 16 - dx - expand, minY - dy - expand, minChunkZ * 16 - dz - expand,
                                    maxChunkX * 16 - dx + expand, maxY - dy + expand, maxChunkZ * 16 - dz + expand);
    }
}
