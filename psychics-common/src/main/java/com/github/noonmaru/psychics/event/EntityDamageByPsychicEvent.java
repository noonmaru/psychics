package com.github.noonmaru.psychics.event;

import com.github.noonmaru.psychics.Ability;
import com.github.noonmaru.psychics.AbilityConcept;
import com.github.noonmaru.psychics.damage.DamageType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityDamageByPsychicEvent extends EntityDamageByEntityEvent {
    private static final HandlerList handlers = new HandlerList();

    private final Ability<? extends AbilityConcept> ability;

    private final DamageType damageType;

    private Location knockbackSource;

    private double knockbackForce;

    public EntityDamageByPsychicEvent(
            @NotNull Entity damager,
            @NotNull Entity damagee,
            double damage,
            @NotNull Ability<? extends AbilityConcept> ability,
            @NotNull DamageType damageType,
            @Nullable Location knockbackSource,
            double knockbackForce
    ) {
        super(damager, damagee, DamageCause.ENTITY_ATTACK, damage);

        this.ability = ability;
        this.damageType = damageType;
        this.knockbackSource = knockbackSource;
        this.knockbackForce = knockbackForce;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    @NotNull
    public Ability<? extends AbilityConcept> getAbility() {
        return ability;
    }

    @NotNull
    public DamageType getDamageType() {
        return damageType;
    }

    @NotNull
    public Location getKnockbackSource() {
        return knockbackSource;
    }

    public void setKnockbackSource(@Nullable Location knockbackSource) {
        this.knockbackSource = knockbackSource;
    }

    public double getKnockbackForce() {
        return knockbackForce;
    }

    public void setKnockbackForce(double knockbackForce) {
        this.knockbackForce = knockbackForce;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
