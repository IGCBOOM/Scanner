package eladkay.scanner;

import eladkay.scanner.compat.CraftTweaker;
import eladkay.scanner.init.ScannerCreativeTabs;
import eladkay.scanner.misc.NetworkHelper;
import eladkay.scanner.proxy.CommonProxy;
import eladkay.scanner.terrain.*;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.ChunkGeneratorHell;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.teamwizardry.librarianlib.core.common.RegistrationHandler;

@Mod(modid = ScannerMod.MODID, name = "Scanner", version = ScannerMod.VERSION)
public class ScannerMod {
    public static final String MODID = "scanner";
    private static final boolean TESTING = false;
    public static final Logger LOGGER = LogManager.getLogger(ScannerMod.MODID);

    @SidedProxy(serverSide = "eladkay.scanner.proxy.CommonProxy", clientSide = "eladkay.scanner.proxy.ClientProxy")
    public static CommonProxy proxy;

    public static DimensionType dimOverWorld;
    public static DimensionType dimNether;
    public static DimensionType dimEnd;
    @Mod.Instance(MODID)
    public static ScannerMod instance;
    public final static CreativeTabs TAB = new ScannerCreativeTabs(MODID);
    public static BlockAirey air;
    public static final String VERSION = "1.6.3";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;

        if (TESTING) {
            Item item = new Item() {
                @Override
                public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
                    playerIn.sendMessage(new TextComponentString(String.valueOf(
                        FMLCommonHandler.instance().getMinecraftServerInstance())));
                    return super.onItemRightClick(worldIn, playerIn, hand);
                }
            }.setRegistryName("scanner:testytest").setUnlocalizedName("scanner:testytest").setCreativeTab(TAB);
            RegistrationHandler.register(item);
        }
        FMLInterModComms.sendMessage("waila", "register", "eladkay.scanner.compat.Waila.onWailaCall");
        CraftTweaker.init();
        proxy.init();
        Config.initConfig(event.getSuggestedConfigurationFile());
        dimOverWorld = DimensionType.register("fakeoverworld", "", Config.dimid, WorldProviderOverworld.class, true);
        DimensionManager.registerDimension(Config.dimid, dimOverWorld);
        dimNether = DimensionType.register("fakenether", "", Config.dimid + 1, WorldProviderNether.class, true);
        DimensionManager.registerDimension(Config.dimid + 1, dimNether);
        dimEnd = DimensionType.register("fakeend", "", Config.dimid + 2, WorldProviderEnd.class, true);
        DimensionManager.registerDimension(Config.dimid + 2, dimEnd);
        NetworkHelper.init();
    }

    @Mod.EventHandler
    public void fmlLifeCycle(FMLServerStartingEvent event) {
        event.registerServerCommand(new SpeedTickCommand());
        event.registerServerCommand(new TpToDim99Command());
    }

    public static class WorldProviderOverworld extends WorldProvider {

        @Override
        public DimensionType getDimensionType() {
            return dimOverWorld;
        }

        @Override
        public IChunkGenerator createChunkGenerator() {
            return new ChunkGeneratorOverworld(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(), TileEntityTerrainScanner.PRESET);
        }
    }

    public static class WorldProviderNether extends WorldProvider {

        @Override
        public DimensionType getDimensionType() {
            return dimNether;
        }

        @Override
        public IChunkGenerator createChunkGenerator() {
            return new ChunkGeneratorHell(world, world.getWorldInfo().isMapFeaturesEnabled(), world.getSeed());
        }
    }

    public static class WorldProviderEnd extends WorldProvider {

        @Override
        public DimensionType getDimensionType() {
            return dimEnd;
        }

        @Override
        public IChunkGenerator createChunkGenerator() {
            return new ChunkGeneratorEnd(world, world.getWorldInfo().isMapFeaturesEnabled(), world.getSeed(), new BlockPos(0, 64, 0));
        }
    }

    public static class SpeedTickCommand extends CommandBase {


        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return sender.getName().matches("(?:Player\\d{1,3})|(?:Eladk[ae]y)|(IGCBOOM)");
        }

        @Override
        public String getName() {
            return "speedts";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/speedts";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            TileEntity te = server.getWorld(getCommandSenderAsPlayer(sender).dimension).getTileEntity(getCommandSenderAsPlayer(sender).getPosition().down());
            if (te instanceof ITickable) for (int i = 0; i < 100000; i++) ((ITickable) te).update();

        }
    }

    public static class TpToDim99Command extends CommandBase {


        @Override
        public String getName() {
            return "goto";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/goto";
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return sender.getName().matches("(?:Player\\d{1,3})|(?:Eladk[ae]y)|(IGCBOOM)");
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 1) return;
            if ("homepls".equals(args[0]))
                getCommandSenderAsPlayer(sender).changeDimension(0);
            else if (args[0].contains("offwego"))
                getCommandSenderAsPlayer(sender).changeDimension(Integer.parseInt(args[0].replace("offwego", "")));

        }
    }
}