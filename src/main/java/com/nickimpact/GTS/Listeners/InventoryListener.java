package com.nickimpact.GTS.Listeners;

import com.nickimpact.GTS.GTS;
import com.nickimpact.GTS.Inventories.Admin;
import com.nickimpact.GTS.Inventories.LotUI;
import com.nickimpact.GTS.Inventories.Main;
import com.nickimpact.GTS.Inventories.PlayerListings;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.common.item.inventory.custom.CustomInventory;

import java.util.Optional;

/**
 * Created by Nick on 12/15/2016.
 */
public class InventoryListener {

    @Listener
    public void onClickEvent(ClickInventoryEvent event, @Root Player p){
        if(event.getTargetInventory().getName().get().contains("GTS | Page") || event.getTargetInventory().getName().get().equalsIgnoreCase("GTS | Search")){
            Main.handleClickEvent(event, p);
        } else if(event.getTargetInventory().getName().get().equalsIgnoreCase("GTS | Confirm")){
            LotUI.handleClickEvent(event, p);
        } else if(event.getTargetInventory().getName().get().equalsIgnoreCase("GTS | Admin")){
            Admin.handleClickEvent(event, p);
        } else if(event.getTargetInventory().getName().get().equalsIgnoreCase("GTS | Your Listings")){
            PlayerListings.handleClickEvent(event, p);
        }
    }

    @Listener
    public void onDropEvent(DropItemEvent event, @Root Player p) {
        Optional<Container> inv = p.getOpenInventory();
        if (inv.isPresent()) {
            if (p.getOpenInventory().get().getName().get().contains("GTS | Page") || p.getOpenInventory().get().getName().get().equalsIgnoreCase("GTS | Search")) {
                event.setCancelled(true);
                Main.showGUI(p, Main.getCurrPage(p), Main.getCurrSearch(p), Main.getPokemon(p));
            } else if (p.getOpenInventory().get().getName().get().equalsIgnoreCase("GTS | Confirm")) {
                event.setCancelled(true);
                LotUI.showGUI(p, LotUI.getCurrLot(p), LotUI.getCurrSearch(p), LotUI.getPokemon(p), LotUI.getIsAdmin(p));
            } else if (p.getOpenInventory().get().getName().get().equalsIgnoreCase("GTS | Admin")) {
                event.setCancelled(true);
                Admin.showGUI(p, Admin.getCurrPage(p));
            } else if (p.getOpenInventory().get().getName().get().equalsIgnoreCase("GTS | Your Listings")) {
                event.setCancelled(true);
                PlayerListings.showGUI(p, PlayerListings.getCurrPage(p), PlayerListings.getCurrSearch(p), PlayerListings.getPokemon(p));
            }
        }
    }

    @Listener
    public void onCloseEvent(InteractInventoryEvent.Close event, @Root Player p){
        if(event.getTargetInventory() instanceof CustomInventory) {
            if (event.getTargetInventory().getName().get().contains("GTS | Page") || event.getTargetInventory().getName().get().equalsIgnoreCase("GTS | Search")) {
                Main.handleCloseEvent(p);
            } else if(event.getTargetInventory().getName().get().equalsIgnoreCase("GTS | Confirm")){
                LotUI.handleCloseEvent(p);
            } else if(event.getTargetInventory().getName().get().equalsIgnoreCase("GTS | Admin")){
                Admin.handleCloseEvent(p);
            } else if(event.getTargetInventory().getName().get().equalsIgnoreCase("GTS | Your Listings")){
                PlayerListings.handleCloseEvent(p);
            }
        }
    }
}
