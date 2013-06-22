package mods.nurseangel.singlebiomecreater;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.stats.StatList;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.Configuration;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.GameRegistry;

public class SingleBiomeCreaterKeyHandler extends KeyHandler {

	// コンフィグの名前とか
	public static final String bindingKey = "KEY_NEXT_BIOME";
	public static final String bindingKeyRev = "KEY_PREV_BIOME";
	public static final String bindingKeyDisp = "KEY_DISP_BIOME";
	public static final String nowBiome = "nowBiome";
	public static final String nowBiomeDefault = "ALL";

	// 現在のバイオーム名
	private String nowBiomeName = null;
	// バイオームID
	private int nowBiomeID = -1;

	// 全バイオームリスト
	private BiomeGenBase[] allBiomeList;
	// コンフィグファイル
	private Configuration configFile;

	// コンストラクタ
	public SingleBiomeCreaterKeyHandler(KeyBinding[] keyBindings, boolean[] repeatings, String startBiome, Configuration config) {
		super(keyBindings, repeatings);

		// コンフィグファイル
		this.configFile = config;

		// 全バイオームリスト NULLも入ってる
		allBiomeList = BiomeGenBase.biomeList;

		// 初期値をチェック
		if (startBiome == null) {
			return;
		}
		if (startBiome.equals(nowBiomeDefault)) {
			this.nowBiomeName = startBiome;
			return;
		}

		// 全バイオームでくるくる
		for (BiomeGenBase biome : allBiomeList) {
			if (biome == null) {
				continue;
			} // リストは大きめに取ってあるので後半nullが入ってる

			// 見つかったらバイオーム初期値にその名をセット
			if (biome.biomeName.equals(startBiome)) {
				this.nowBiomeName = startBiome;
				this.nowBiomeID = biome.biomeID;
				break;
			}
		}
		if (nowBiomeName == null) {
			// バイオームリストに引数がなかった場合、ALLにしておく
			this.nowBiomeName = nowBiomeDefault;
			this.setConfigNowBiome(nowBiomeDefault);
			return;
		}

		// 全バイオームを消去して、該当の一つだけバイオームを登録
		removeAllBiome();
		addBiome(nowBiomeID);
	}

	// コンフィグを引数のバイオームにセットして保存
	private void setConfigNowBiome(String biomeName) {
		// 強制的に上書きしてるが、これが正しい方法なのかは不明
		configFile.getCategory(Configuration.CATEGORY_GENERAL).get(nowBiome).set(biomeName);
		configFile.save();
	}

	// ？
	@Override
	public String getLabel() {
		return "SingleBiomeCreaterKeyHandler";
	}

	/**
	 * キーを押したときに動作
	 */
	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
		// 一度押しただけでtrue,falseの2回よばれる
		if (tickEnd != true) {
			return;
		}

		/*
		 * 押されたキーに対応した処理 TODO getKeyBindingsでキーバインド拾えるからそっちと照合した方がいいような
		 */
		int hoge = kb.keyCode;
		if (kb.keyCode == SingleBiomeCreater.bindingKey) {
			keyDownNewBiome();
		} else if (kb.keyCode == SingleBiomeCreater.bindingKeyDisp) {
			displayNowBiome();
		}
	}

	/**
	 * 次/前のバイオームへボタンを押した
	 */
	public void keyDownNewBiome() {
		Minecraft minecraft = FMLClientHandler.instance().getClient();

		KeyBinding[] hoge = getKeyBindings();

		// 次のバイオーム
		int nextBiomeID = setNextBiome(nowBiomeID);
		String nextBiomeName;
		if (nextBiomeID < 0) {
			nextBiomeName = nowBiomeDefault;
		} else {
			nextBiomeName = allBiomeList[nextBiomeID].biomeName;
		}

		// 次のバイオームを現在のバイオームに上書き
		nowBiomeID = nextBiomeID;
		nowBiomeName = nextBiomeName;

		// 現在のバイオーム名をコンフィグに保存
		this.setConfigNowBiome(nowBiomeName);

		/*
		 * 一応できてはいるのだが、バイオーム生成に使うバイオームキャッシュはログイン時のものを保持しているみたい？で、
		 * ゲーム中に変えたとしても即反映はされない
		 *
		 * 解決策としては、 ・一度QUITして入り直す ・チャンク生成システムのバイオームキャッシュがどこかにあるはずなのでリセットする
		 * ・ChunkDataEventあたりを乗っ取ってうまいことどうにかする
		 *
		 * あたりでできると思われるが、後ろ二つはよくわからなかった とりあえず強制リスタートという素敵な原始的手段で解決
		 */

		IntegratedServer integratedServer = minecraft.getIntegratedServer();
		minecraft.statFileWriter.readStat(StatList.leaveGameStat, 1);
		minecraft.theWorld.getSaveHandler().saveWorldInfo(minecraft.theWorld.getWorldInfo());
		minecraft.theWorld.sendQuittingDisconnectingPacket();
		minecraft.loadWorld((WorldClient) null);
		minecraft.launchIntegratedServer(integratedServer.getFolderName(), integratedServer.getWorldName(), (WorldSettings) null);

		// TODO リスタート直後にdisplayNowBiome()を呼びたいが、ここで呼ぶとエラーになる
	}

	/**
	 * 現在のバイオームを表示
	 */
	public void displayNowBiome() {
		try {
			Minecraft minecraft = FMLClientHandler.instance().getClient();
			minecraft.thePlayer.addChatMessage("NOW BIOME IS " + nowBiomeName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 何もしない
	@Override
	public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
	}

	// クライアント？
	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.CLIENT);
	}

	// -----------------------------------------------------------------
	// バイオーム設定関連

	// "次の"バイオームをセット
	private int setNextBiome(int biomeID) {

		// -1であれば全バイオーム
		if (biomeID < 0) {
			removeAllBiome();
			addBiome(0);
			return 0;
		}

		// 次のバイオームが存在するならそれをセット
		int nextBiomeID = getnextID(biomeID);
		if (nextBiomeID > 0) {
			removeBiome(biomeID);
			addBiome(nextBiomeID);
			return nextBiomeID;
		}

		// 次がなければ-1に戻る
		removeBiome(biomeID);
		addAllBiome();
		return -1;
	}

	private int getnextID(int biomeID) {
		int nextID = biomeID + 1;

		try {
			if (allBiomeList[nextID] == null) {
				nextID = getnextID(nextID);
			}
		} catch (Exception e) {
			return -1;
		}

		return nextID;
	}

	// 引数のバイオームIDのバイオームをセット
	private void addBiome(int biomeID) {
		GameRegistry.addBiome(allBiomeList[biomeID]);
	}

	// 引数のバイオームIDのバイオームを削除
	private void removeBiome(int biomeID) {
		GameRegistry.removeBiome(allBiomeList[biomeID]);
	}

	// 全バイオームを削除
	private void removeAllBiome() {
		BiomeGenBase[] localAllBiomeList = BiomeGenBase.biomeList;
		for (BiomeGenBase biome : localAllBiomeList) {
			GameRegistry.removeBiome(biome);
		}
	}

	// 全バイオームを追加
	private void addAllBiome() {
		for (BiomeGenBase biome : allBiomeList) {
			GameRegistry.addBiome(biome);
		}
	}

}
