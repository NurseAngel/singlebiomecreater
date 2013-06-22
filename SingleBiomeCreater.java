package mods.nurseangel.singlebiomecreater;

import java.util.logging.Level;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.Configuration;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
public class SingleBiomeCreater {

	// バインドするキー
	public static int bindingKey;
	// public static int bindingKeyRev;
	public static int bindingKeyDisp;
	public static String nowBiome;

	public Configuration cfg;
	public SingleBiomeCreaterKeyHandler onlyBiomeKeyHandler;
	private boolean isEnabled = false;

	// コンストラクタ的なもの
	@Mod.PreInit
	public void modPreInit(FMLPreInitializationEvent event) {

		cfg = new Configuration(event.getSuggestedConfigurationFile());

		try {
			cfg.load();

			// 有効
			isEnabled = cfg.get(Configuration.CATEGORY_GENERAL, "isEnabled", true, "If you want to disable, set false").getBoolean(true);

			if(isEnabled){

				// バインドキー
				String comment = "If you set " + SingleBiomeCreaterKeyHandler.bindingKey + " to 0, you can use this for DISPLAY BIOME MOD";
				bindingKey = cfg.get(Configuration.CATEGORY_GENERAL, SingleBiomeCreaterKeyHandler.bindingKey, Keyboard.KEY_K, comment).getInt();

				comment = "Show now create biome";
				bindingKeyDisp = cfg.get(Configuration.CATEGORY_GENERAL, SingleBiomeCreaterKeyHandler.bindingKeyDisp, Keyboard.KEY_L, comment).getInt();

				// バイオーム初期値
				nowBiome = cfg
						.get(Configuration.CATEGORY_GENERAL, SingleBiomeCreaterKeyHandler.nowBiome, SingleBiomeCreaterKeyHandler.nowBiomeDefault, "Now biome")
						.getString();

			}

		} catch (Exception e) {
			FMLLog.log(Level.SEVERE, Reference.MOD_NAME + " config Load failed... ");
		} finally {
			cfg.save();
		}

	}

	// load()的なもの
	@Mod.PostInit
	public void modInit(FMLPostInitializationEvent event) {

		// 無効
		if (!isEnabled) {
			return;
		}

		/*
		 * バイオーム表示キーだけが有効であればバイオーム表示機能のみ バイオーム切り替えキーが有効であればそちらも
		 */
		if (bindingKey < 1) {
			if (bindingKeyDisp > 0) {
				KeyBinding[] myBinding = { new KeyBinding("bindingKey", bindingKey), new KeyBinding("bindingKeyDisp", bindingKeyDisp) };
				boolean[] myBindingRepeat = { false, false };
				onlyBiomeKeyHandler = new SingleBiomeCreaterKeyHandler(myBinding, myBindingRepeat, nowBiome, cfg);
			}
		} else {
			KeyBinding[] myBinding = { new KeyBinding("bindingKey", bindingKey), new KeyBinding("bindingKeyDisp", bindingKeyDisp) };
			boolean[] myBindingRepeat = { false, false };
			onlyBiomeKeyHandler = new SingleBiomeCreaterKeyHandler(myBinding, myBindingRepeat, nowBiome, cfg);
		}

		// キーハンドラをセット
		KeyBindingRegistry.registerKeyBinding(onlyBiomeKeyHandler);
	}

}
