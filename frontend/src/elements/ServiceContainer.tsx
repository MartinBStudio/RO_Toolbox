import type { ReactNode } from "react";

type ServiceGroup = {
  id: string;
  title: string;
  contents: ReactNode[];
};

type ServiceContainerProps = {
  groups: ServiceGroup[];
};

export function ServiceContainer({ groups }: ServiceContainerProps) {
  return (
    <section className="card">
      <h2>Services</h2>
      <div className="serviceGroups">
        {groups.map((group) => (
          <section key={group.id} className="serviceGroup">
            <h3>{group.title}</h3>
            <div className="serviceGroupContent">
              {group.contents.map((content, index) => (
                <div key={`${group.id}-${index}`} className="serviceGroupItem">
                  {content}
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>
    </section>
  );
}
