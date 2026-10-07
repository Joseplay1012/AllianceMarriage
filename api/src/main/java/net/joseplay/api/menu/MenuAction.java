package net.joseplay.api.menu;

@FunctionalInterface
public interface MenuAction {
    void execute(MenuContext context);
}
