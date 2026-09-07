type ServiceAccordionTitleProps = {
  title: string;
  activeProfileName: string;
  activeProfileAuthor?: string | null;
  activeProfileVersion?: string | null;
  hasInstalledProfile: boolean;
};

export function ServiceAccordionTitle({
  title,
  activeProfileName,
  activeProfileAuthor,
  activeProfileVersion,
  hasInstalledProfile
}: ServiceAccordionTitleProps) {
  return (
    <div className="serviceAccordionTitleBlock">
      <div className="serviceAccordionTitleRow">
        <span className="serviceAccordionAccent" aria-hidden="true" />
        <p className="sectionTitle serviceAccordionTitle">{title}</p>
        <span
          className={`serviceAccordionBadge ${
            hasInstalledProfile ? "serviceAccordionBadgeActive" : "serviceAccordionBadgeInactive"
          }`}
        >
          {hasInstalledProfile ? "Installed" : "Not installed"}
        </span>
      </div>
      <div className="serviceAccordionDetailsRow">
        {hasInstalledProfile ? (
          <p className="serviceAccordionProfileName">
            {activeProfileName}
          </p>
        ) : null}
        {activeProfileAuthor || activeProfileVersion ? (
          <div className="serviceAccordionMeta">
            {activeProfileAuthor ? <span className="serviceAccordionMetaChip">{activeProfileAuthor}</span> : null}
            {activeProfileVersion ? <span className="serviceAccordionMetaChip">{activeProfileVersion}</span> : null}
          </div>
        ) : null}
      </div>
    </div>
  );
}
