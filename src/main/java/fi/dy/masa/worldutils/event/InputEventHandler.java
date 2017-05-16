package fi.dy.masa.worldutils.event;

import org.lwjgl.input.Keyboard;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.worldutils.item.base.IKeyBound;
import fi.dy.masa.worldutils.network.MessageKeyPressed;
import fi.dy.masa.worldutils.network.PacketHandler;
import fi.dy.masa.worldutils.reference.HotKeys;
import fi.dy.masa.worldutils.reference.Keybindings;
import fi.dy.masa.worldutils.util.EntityUtils;
import gnu.trove.map.hash.TIntIntHashMap;

@SideOnly(Side.CLIENT)
public class InputEventHandler
{
    public static final TIntIntHashMap KEY_CODE_MAPPINGS = new TIntIntHashMap(16);
    /** Has the active mouse scroll modifier mask, if any */
    private static int scrollingMask = 0;
    /** Has the currently active/pressed mask of supported modifier keys */
    private static int modifierMask = 0;

    /**
     * Reset the modifiers externally. This is to fix the stuck modifier keys
     * if a GUI is opened while the modifiers are active.
     * FIXME Apparently there are key input events for GUI screens in 1.8,
     * so this probably can be removed then.
     */
    public static void resetModifiers()
    {
        scrollingMask = 0;
        modifierMask = 0;
    }

    public static boolean isHoldingKeyboundItem(EntityPlayer player)
    {
        return EntityUtils.isHoldingItemOfType(player, IKeyBound.class);
    }

    @SubscribeEvent
    public void onKeyInputEvent(InputEvent.KeyInputEvent event)
    {
        EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();
        int eventKey = Keyboard.getEventKey();
        boolean keyState = Keyboard.getEventKeyState();

        // One of our supported modifier keys was pressed or released
        if (KEY_CODE_MAPPINGS.containsKey(eventKey))
        {
            int mask = KEY_CODE_MAPPINGS.get(eventKey);

            // Key was pressed
            if (keyState)
            {
                modifierMask |= mask;

                // Only add scrolling mode mask if the currently selected item is one of our IKeyBound items
                if (isHoldingKeyboundItem(player) == true)
                {
                    scrollingMask |= mask;
                }
            }
            // Key was released
            else
            {
                modifierMask &= ~mask;
                scrollingMask &= ~mask;
            }
        }

        // In-game (no GUI open)
        if (FMLClientHandler.instance().getClient().inGameHasFocus)
        {
            if (eventKey == Keybindings.keyToggleMode.getKeyCode() && keyState)
            {
                if (isHoldingKeyboundItem(player))
                {
                    int keyCode = HotKeys.KEYBIND_ID_TOGGLE_MODE | modifierMask;
                    PacketHandler.INSTANCE.sendToServer(new MessageKeyPressed(keyCode));
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event)
    {
        int dWheel = event.getDwheel();

        if (dWheel != 0)
        {
            dWheel /= 120;

            // If the player pressed down a modifier key while holding an IKeyBound item
            // (note: this means it specifically WON'T work if the player started pressing a modifier
            // key while holding something else, for example when scrolling through the hotbar!!),
            // then we allow for easily scrolling through the changeable stuff using the mouse wheel.
            if (scrollingMask != 0)
            {
                EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();

                if (isHoldingKeyboundItem(player))
                {
                    int key = HotKeys.KEYCODE_SCROLL | scrollingMask;

                    // Scrolling up, reverse the direction.
                    if (dWheel > 0)
                    {
                        key |= HotKeys.SCROLL_MODIFIER_REVERSE;
                    }

                    if (event.isCancelable() == true)
                    {
                        event.setCanceled(true);
                    }

                    PacketHandler.INSTANCE.sendToServer(new MessageKeyPressed(key));
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiOpenEvent(GuiOpenEvent event)
    {
        // Reset the scrolling modifier when the player opens a GUI.
        // Otherwise the key up event will get eaten and our scrolling mode will get stuck on
        // until the player sneaks again.
        // FIXME Apparently there are key input events for GUI screens in 1.8,
        // so this probably can be removed then.
        resetModifiers();
    }

    static
    {
        KEY_CODE_MAPPINGS.put(Keyboard.KEY_LSHIFT,      HotKeys.KEYBIND_MODIFIER_SHIFT);
        KEY_CODE_MAPPINGS.put(Keyboard.KEY_RSHIFT,      HotKeys.KEYBIND_MODIFIER_SHIFT);
        KEY_CODE_MAPPINGS.put(Keyboard.KEY_LCONTROL,    HotKeys.KEYBIND_MODIFIER_CONTROL);
        KEY_CODE_MAPPINGS.put(Keyboard.KEY_RCONTROL,    HotKeys.KEYBIND_MODIFIER_CONTROL);
        KEY_CODE_MAPPINGS.put(Keyboard.KEY_LMENU,       HotKeys.KEYBIND_MODIFIER_ALT);
        KEY_CODE_MAPPINGS.put(Keyboard.KEY_RMENU,       HotKeys.KEYBIND_MODIFIER_ALT);
    }
}
