package seraphina.silent_love_sword_trial.badmc;


import net.minecraftforge.common.ForgeInternalHandler;
import net.minecraftforge.common.loot.LootModifierManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.BusBuilderImpl;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBusInvokeDispatcher;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.server.command.ConfigCommand;
import net.minecraftforge.server.command.ForgeCommand;

public class SilentEventBus extends EventBus {
    public SilentEventBus(BusBuilderImpl busBuilder) {
        super(busBuilder);
    }

    @Override
    public boolean post(Event event) {
        return this.post(event, (IEventListener::invoke));
    }

    @Override
    public boolean post(Event event, IEventBusInvokeDispatcher wrapper) {
        if (event instanceof RegisterCommandsEvent e) {
            new ForgeCommand(e.getDispatcher());
            ConfigCommand.register(e.getDispatcher());
        }
        if (event instanceof AddReloadListenerEvent e) {
            try {
                ForgeInternalHandler.INSTANCE = new LootModifierManager();
                e.addListener(ForgeInternalHandler.INSTANCE);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return event.isCancelable() && event.isCanceled();
    }

    @Override
    public void shutdown() {}
}
