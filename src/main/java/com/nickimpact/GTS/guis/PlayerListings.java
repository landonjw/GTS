package com.nickimpact.GTS.guis;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nickimpact.GTS.GTS;
import com.nickimpact.GTS.configuration.MessageConfig;
import com.nickimpact.GTS.utils.Lot;
import com.nickimpact.GTS.utils.LotCache;
import com.nickimpact.GTS.utils.PokemonItem;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.List;
import java.util.Optional;

public class PlayerListings extends InventoryBase {

    private int page;
    private int maxPage;

    public PlayerListings(Player player, int page) {
        super(6, Text.of(
                TextColors.RED, "GTS", TextColors.DARK_GRAY, " \u00bb ",
                TextColors.DARK_GREEN, "Your Listings"
        ));

        this.page = page;

        List<LotCache> valid = Lists.newArrayList();
        GTS.getInstance().getLots().forEach(lot -> {
            if(lot.getLot().getOwner().equals(player.getUniqueId()))
                valid.add(lot);
        });


        this.maxPage = valid.size() % 42 == 0 && valid.size() / 42 != 0?
                valid.size() / 42 :
                valid.size() / 42 + 1;

        this.setupDesign();
        this.setupListings(player);
    }

    private void setupDesign(){
        for(int x = 7, y = 0; y < 6; y++){
            this.addIcon(SharedItems.forgeBorderIcon(x + (9 * y), DyeColors.BLACK));
        }

        InventoryIcon nextPage = SharedItems.pageIcon(8, true, page, page < maxPage ? page + 1 : 1);
        nextPage.addListener(ClickInventoryEvent.class, e -> {
            Player p = e.getCause().first(Player.class).get();
            Sponge.getScheduler().createTaskBuilder()
                    .execute(() -> this.updatePage(p, true))
                    .delayTicks(1)
            .submit(GTS.getInstance());
        });
        this.addIcon(nextPage);

        InventoryIcon lastPage = SharedItems.pageIcon(17, false, page, page > 1 ? page - 1 : maxPage);
        nextPage.addListener(ClickInventoryEvent.class, e -> {
            Player p = e.getCause().first(Player.class).get();
            Sponge.getScheduler().createTaskBuilder()
                    .execute(() -> this.updatePage(p, false))
                    .delayTicks(1)
            .submit(GTS.getInstance());
        });
        this.addIcon(lastPage);

        InventoryIcon refreshIcon = SharedItems.refreshIcon(35);
        refreshIcon.addListener(ClickInventoryEvent.class, e -> {
            Player p = e.getCause().first(Player.class).get();
            this.setupListings(p);
            this.updateContents();
        });
        this.addIcon(refreshIcon);

        InventoryIcon lastMenuIcon = SharedItems.lastMenu(53);
        lastMenuIcon.addListener(ClickInventoryEvent.class, e -> {
            Sponge.getScheduler().createTaskBuilder().execute(() -> {
                Player p = e.getCause().first(Player.class).get();

                p.closeInventory(Cause.of(NamedCause.source(GTS.getInstance())));
                p.openInventory(new MainUI(p, 1, false, Lists.newArrayList(), Maps.newHashMap()).getInventory(),
                                Cause.of(NamedCause.source(GTS.getInstance())));
            }).delayTicks(1).submit(GTS.getInstance());
        });
        this.addIcon(lastMenuIcon);
    }

    private void setupListings(Player player) {
        int index = (this.page - 1) * 42;

        List<LotCache> valid = Lists.newArrayList();
        GTS.getInstance().getLots().forEach(lot -> {
            if (lot.getLot().getOwner().equals(player.getUniqueId()))
                valid.add(lot);
        });

        for(int x = 0, y = 0; y < 6; index++){
            if(x == 7){
                x = 0;
                y++;
            }

            if(index >= valid.size()) {
                this.getAllIcons().remove(x + (9 * y));
                x++;
                continue;
            }

            if(valid.get(index).isExpired()) continue;
            Lot lot = valid.get(index).getLot();
            PokemonItem item = lot.getItem();
            InventoryIcon pokemon = new InventoryIcon(x + (9 * y), item.getItem(valid.get(index)));
            pokemon.addListener(ClickInventoryEvent.class, e -> {
                Player p = e.getCause().first(Player.class).get();

                if(!e.getCursorTransaction().getFinal().getType().equals(ItemTypes.NONE)) {
                    String lotID = e.getCursorTransaction().getFinal().get(Keys.ITEM_LORE).get().get(0).toPlain();
                    Optional<LotCache> lotCache = GTS.getInstance().getLots().stream().filter(l -> l.getLot().getLotID() == Integer.valueOf(lotID.substring(lotID.indexOf(": ") + 2))).findFirst();
                    if(!lotCache.isPresent()){
                        for(Text text : MessageConfig.getMessages("Generic.Purchase.Error.Already Sold", null))
                            p.sendMessage(text);
                    } else {
                        Sponge.getScheduler().createTaskBuilder().execute(() -> {
                            p.closeInventory(Cause.of(NamedCause.source(GTS.getInstance())));
                            p.openInventory(new LotUI(p, lotCache.get(), this.page, false, Lists.newArrayList(), Maps.newHashMap(), false).getInventory(),
                                            Cause.of(NamedCause.source(GTS.getInstance())));

                        }).delayTicks(1).submit(GTS.getInstance());
                    }
                }
            });
            this.addIcon(pokemon);
            x++;
        }
    }

    /**
     * Updates the page of the inventory, then proceeds to update displayed listings
     *
     * @param upOrDown True = Page Up, False = Page Down
     */
    private void updatePage(Player player, boolean upOrDown) {
        if(upOrDown){
            if(this.page < maxPage)
                ++this.page;
            else
                this.page = 1;
        } else {
            if(this.page > 1)
                --this.page;
            else
                this.page = maxPage;
        }

        InventoryIcon nextPage = SharedItems.pageIcon(8, true, page, page < maxPage ? page + 1 : 1);
        nextPage.addListener(ClickInventoryEvent.class, e -> {
            Player p = e.getCause().first(Player.class).get();
            updatePage(p, true);
        });
        this.addIcon(nextPage);

        InventoryIcon lastPage = SharedItems.pageIcon(17, false, page, page > 1 ? page - 1 : maxPage);
        nextPage.addListener(ClickInventoryEvent.class, e -> {
            Player p = e.getCause().first(Player.class).get();
            updatePage(p, false);
        });
        this.addIcon(lastPage);

        this.setupListings(player);
        this.updateContents();
    }
}