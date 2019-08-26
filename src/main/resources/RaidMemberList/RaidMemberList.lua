StaticPopupDialogs["RAID_MEMBER_LIST"] = {
    text = "Raid Members:",
    button1 = "Done",
    hasEditBox = true,
    editBoxWidth = 600,
    timeout = 0,
    whileDead = true,
    hideOnEscape = true,
    preferredIndex = 3,
    OnShow = function (self, data)
        message = ""

        for i = 1, GetNumRaidMembers() do
            name, rank, subgroup, level, class, fileName, zone, online, isDead, role, isML = GetRaidRosterInfo(i);
            if string.len(message) > 0 then
                message = message .. " " .. name;
            else 
                message = name;
            end
        end

        self.editBox:SetText(message)
    end,
}; 

function RaidMemberList_SlashCommandHandler( msg )
    DEFAULT_CHAT_FRAME:AddMessage( "Showing raid members" ); -- Output the message to the default chat window
    StaticPopup_Show("RAID_MEMBER_LIST")
end

SlashCmdList["RAIDMEMBERLIST"] = RaidMemberList_SlashCommandHandler; 
SLASH_RAIDMEMBERLIST1 = "/raidmembers"; 
