export function FloppyDiskIcon({className = "heroIcon"}: {className?: string}) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round"
             className={className} aria-hidden="true">
            <path d="M16.5 3.75V6.75a1.5 1.5 0 0 1-1.5 1.5H9a1.5 1.5 0 0 1-1.5-1.5V3.75"/>
            <path d="M3.75 6.75A2.25 2.25 0 0 1 6 4.5h10.379a2.25 2.25 0 0 1 1.59.659l2.372 2.372a2.25 2.25 0 0 1 .659 1.591V18A2.25 2.25 0 0 1 18.75 20.25H5.25A2.25 2.25 0 0 1 3 18V6.75z"/>
            <path d="M7.5 20.25V14.25A1.5 1.5 0 0 1 9 12.75h6a1.5 1.5 0 0 1 1.5 1.5v6"/>
        </svg>
    );
}
