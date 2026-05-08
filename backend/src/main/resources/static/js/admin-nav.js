(function () {
    /**
     * @date 2026-05-08
     * @desc 관리자 사이드 메뉴 아코디언의 열림 상태와 접근성 속성을 적용합니다.
     */
    function applyAccordionState(group, open) {
        const toggleButton = group.querySelector('.admin-side-nav-group-toggle');
        const itemArea = group.querySelector('.admin-side-nav-group-items');

        group.classList.toggle('is-collapsed', !open);
        if (toggleButton) {
            toggleButton.setAttribute('aria-expanded', String(open));
        }
        if (itemArea) {
            itemArea.hidden = !open;
        }
    }

    /**
     * @date 2026-05-08
     * @desc 선택한 아코디언 그룹을 열고 나머지 그룹을 닫습니다.
     */
    function openOnlySelectedGroup(groups, selectedGroup) {
        groups.forEach((group) => {
            applyAccordionState(group, group === selectedGroup);
        });
    }

    /**
     * @date 2026-05-08
     * @desc 현재 활성 메뉴 기준으로 관리자 사이드 메뉴 아코디언을 초기화합니다.
     */
    function bindAdminSideNavAccordion() {
        const groups = Array.from(document.querySelectorAll('.admin-side-nav-group[data-accordion-group="true"]'));
        if (groups.length === 0) {
            return;
        }

        groups.forEach((group) => {
            if (group.dataset.accordionInitialized === 'true') {
                return;
            }

            group.dataset.accordionInitialized = 'true';
            const defaultOpen = group.dataset.accordionDefaultOpen === 'true';
            const toggleButton = group.querySelector('.admin-side-nav-group-toggle');

            applyAccordionState(group, defaultOpen);

            if (!toggleButton) {
                return;
            }

            toggleButton.addEventListener('click', () => {
                const willOpen = group.classList.contains('is-collapsed');
                if (willOpen) {
                    openOnlySelectedGroup(groups, group);
                    return;
                }
                applyAccordionState(group, false);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', bindAdminSideNavAccordion);
})();
