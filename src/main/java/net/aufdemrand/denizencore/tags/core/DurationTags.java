package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.Duration;
import net.aufdemrand.denizencore.objects.TagRunnable;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;

public class DurationTags {

    public DurationTags() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                durationTags(event);
            }
        }, "duration");
    }

    public void durationTags(ReplaceableTagEvent event) {

        if (!event.matches("duration") || event.replaced()) {
            return;
        }

        Duration duration = null;

        if (event.hasNameContext()) {
            duration = Duration.valueOf(event.getNameContext(), event.getAttributes().context);
        }

        if (duration == null) {
            return;
        }

        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(duration, attribute.fulfill(1)));

    }
}
