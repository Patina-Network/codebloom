package org.patinanetwork.codebloom.common.schools;

import org.patinanetwork.codebloom.common.db.models.usertag.Tag;

public enum SchoolEnum {
    HUNTER("@stu-mail.hunter.cuny.edu", Tag.Hunter),
    NYU("@nyu.edu", Tag.Nyu),
    BARUCH("@stu-mail.baruch.cuny.edu", Tag.Baruch),
    RPI("@rpi.edu", Tag.Rpi),
    SBU("@stonybrook.edu", Tag.Sbu),
    CCNY("@stu-mail.ccny.cuny.edu", Tag.Ccny),
    COLUMBIA("@columbia.edu", Tag.Columbia),
    CORNELL("@cornell.edu", Tag.Cornell),
    BMCC("@stu-mail.bmcc.cuny.edu", Tag.Bmcc);

    private final String emailDomain;
    private final Tag internalTag;

    SchoolEnum(final String emailDomain, final Tag internalTag) {
        this.emailDomain = emailDomain;
        this.internalTag = internalTag;
    }

    public String getEmailDomain() {
        return emailDomain;
    }

    public Tag getInternalTag() {
        return internalTag;
    }
}
