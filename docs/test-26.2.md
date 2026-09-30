# Manual regression checks (Minecraft 26.2)

Install matching Fabric Loader and Fabric API, then use the jar from `build/libs`.
Compilation does not verify rendering or mixin application at game startup.

## Advancement browser

- Open the browser with L in a world that has advancements.
- Try different window sizes and GUI scales. Check that cards and controls stay
  inside the panel and never overlap the page buttons.
- Check a large window: the grid should be able to show three or four columns.
- Search for a title, clear the search, change sort and hide completed entries.
- Go to the last page, resize the window, and check that the page remains valid.
- Search for a nonexistent title: the empty state must not show page 0.
- Type in the search box: arrow keys must not unexpectedly change pages.
- Check category chips, descriptions on hover, percentages and page buttons.

## Achievement banner

- Unlock a normal advancement and a challenge. Check the icon, title and sound.
- Unlock an advancement with a very long title, including from another mod:
  the title should end with an ellipsis rather than escape the banner.
- Unlock several advancements in succession: banners should have enough vertical
  space and must leave room for other vanilla notifications.
- Change the notification display-time setting. Check that the banner respects
  that setting while retaining its entrance and exit animations.
- Confirm the vanilla advancement toast is replaced, not shown twice.
