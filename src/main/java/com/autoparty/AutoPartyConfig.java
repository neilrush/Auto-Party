/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2025, neilrush <https://github.com/neilrush/>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.autoparty;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("autopartyplugin")
public interface AutoPartyConfig extends Config
{
    @ConfigItem(
        keyName = "showPassphrase",
        name = "Show Passphrase on Login",
        description = "Shows the party passphrase in the chat when the user logs in.",
        hidden = false)
    default boolean showPassphrase()
    {
        return false;
    }
    @ConfigItem(
        keyName = "joinedParty",
        name = "Joined Party",
        description = "Has the user joined a party while the plugin was enabled.",
        hidden = true)
    default boolean joinedParty()
    {
        return false;
    }

    @ConfigItem(
        keyName = "joinedParty",
        name = "Joined Party",
        description = "Has the user joined a party while the plugin was enabled.",
        hidden = true)
    void setJoinedParty(boolean joinedParty);
}

