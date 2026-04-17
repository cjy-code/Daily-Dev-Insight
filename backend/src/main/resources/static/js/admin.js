(function () {
    /**
     * @date 2026-04-15
     * @desc 템플릿 편집 버튼 이벤트를 등록합니다.
     */
    function bindTemplateLoadButtons() {
        const buttons = document.querySelectorAll('.load-template-btn');
        buttons.forEach((button) => {
            button.addEventListener('click', () => {
                setPromptFormValue(button.dataset);
                updatePromptTemplateSaveButtonState();
            });
        });
    }

    /**
     * @date 2026-04-15
     * @desc 템플릿 데이터셋으로 편집 폼 값을 세팅합니다.
     */
    function setPromptFormValue(dataset) {
        const idField = document.getElementById('promptId');
        const nameField = document.getElementById('promptName');
        const descriptionField = document.getElementById('promptDescription');
        const contentField = document.getElementById('templateContent');

        idField.value = dataset.id || '';
        nameField.value = dataset.name || '';
        descriptionField.value = dataset.description || '';
        contentField.value = dataset.content || '';
    }

    /**
     * @date 2026-04-15
     * @desc 프롬프트 입력 폼 초기화 버튼 이벤트를 등록합니다.
     */
    function bindResetButton() {
        const resetButton = document.getElementById('resetPromptForm');
        if (!resetButton) {
            return;
        }

        resetButton.addEventListener('click', () => {
            const form = document.getElementById('promptTemplateForm');
            form.reset();
            document.getElementById('promptId').value = '';
            updatePromptTemplateSaveButtonState();
        });
    }

    /**
     * @date 2026-04-16
     * @desc 템플릿 개수 제한에 따라 저장 버튼 활성 상태를 갱신합니다.
     */
    function updatePromptTemplateSaveButtonState() {
        const promptPanel = document.querySelector('.generation-tab-panel[data-tab-panel="prompt"]');
        const saveButton = document.getElementById('savePromptTemplateButton');
        const promptIdField = document.getElementById('promptId');
        if (!promptPanel || !saveButton || !promptIdField) {
            return;
        }

        const templateCount = Number(promptPanel.dataset.templateCount || 0);
        const maxTemplateCount = Number(promptPanel.dataset.maxTemplateCount || 10);
        const isCreateMode = !promptIdField.value;
        const shouldDisable = isCreateMode && templateCount >= maxTemplateCount;
        saveButton.disabled = shouldDisable;
        saveButton.title = shouldDisable ? '템플릿은 최대 10개까지 등록할 수 있습니다.' : '';
    }

    /**
     * @date 2026-04-16
     * @desc 템플릿 저장 폼 입력 상태 변경 시 저장 버튼 상태를 갱신합니다.
     */
    function bindPromptTemplateSaveLimitGuard() {
        const promptIdField = document.getElementById('promptId');
        const promptForm = document.getElementById('promptTemplateForm');
        if (!promptIdField || !promptForm) {
            return;
        }

        promptIdField.addEventListener('input', updatePromptTemplateSaveButtonState);
        promptForm.addEventListener('reset', () => {
            window.setTimeout(updatePromptTemplateSaveButtonState, 0);
        });
        updatePromptTemplateSaveButtonState();
    }

    /**
     * @date 2026-04-16
     * @desc URL 해시에서 생성관리 탭 키를 파싱합니다.
     */
    function getGenerationTabKeyFromHash() {
        const hashValue = window.location.hash || '';
        const prefix = '#generation-';
        if (!hashValue.startsWith(prefix)) {
            return '';
        }
        return hashValue.replace(prefix, '');
    }

    /**
     * @date 2026-04-16
     * @desc 현재 활성 프롬프트 존재 여부를 반환합니다.
     */
    function hasActivePromptTemplate() {
        const promptPanel = document.querySelector('.generation-tab-panel[data-tab-panel="prompt"]');
        if (!promptPanel) {
            return false;
        }
        return promptPanel.dataset.hasActiveTemplate === 'true';
    }

    /**
     * @date 2026-04-16
     * @desc 지정한 생성관리 탭을 활성화하고 나머지 패널을 숨깁니다.
     */
    function activateGenerationTab(tabKey) {
        const tabButtons = document.querySelectorAll('.admin-tab-btn[data-tab-target]');
        const tabPanels = document.querySelectorAll('.generation-tab-panel[data-tab-panel]');
        if (tabButtons.length === 0 || tabPanels.length === 0) {
            return;
        }

        let selectedTabKey = tabKey;
        const hasTargetButton = Array.from(tabButtons).some((button) => button.dataset.tabTarget === tabKey);
        if (!hasTargetButton) {
            selectedTabKey = tabButtons[0].dataset.tabTarget;
        }

        tabButtons.forEach((button) => {
            const isActive = button.dataset.tabTarget === selectedTabKey;
            button.classList.toggle('active', isActive);
            button.setAttribute('aria-selected', String(isActive));
        });

        tabPanels.forEach((panel) => {
            const isActive = panel.dataset.tabPanel === selectedTabKey;
            panel.hidden = !isActive;
        });

        window.history.replaceState(null, '', '#generation-' + selectedTabKey);
    }

    /**
     * @date 2026-04-16
     * @desc 활성 프롬프트가 없을 때 수동/예약 탭 버튼 상태를 갱신합니다.
     */
    function updateGenerationTabAccessState() {
        const tabButtons = document.querySelectorAll('.admin-tab-btn[data-requires-active-template="true"]');
        const canUseGenerationTabs = hasActivePromptTemplate();
        tabButtons.forEach((button) => {
            button.classList.toggle('disabled', !canUseGenerationTabs);
            button.setAttribute('aria-disabled', String(!canUseGenerationTabs));
            button.title = canUseGenerationTabs ? '' : '활성 프롬프트 템플릿이 없어 사용할 수 없습니다.';
        });
    }

    /**
     * @date 2026-04-16
     * @desc 생성관리 탭 버튼 클릭 이벤트를 등록합니다.
     */
    function bindGenerationTabs() {
        const tabButtons = document.querySelectorAll('.admin-tab-btn[data-tab-target]');
        if (tabButtons.length === 0) {
            return;
        }

        updateGenerationTabAccessState();
        const initialTabKey = getGenerationTabKeyFromHash() || tabButtons[0].dataset.tabTarget;
        activateGenerationTab(initialTabKey);

        tabButtons.forEach((button) => {
            button.addEventListener('click', () => {
                const requiresActiveTemplate = button.dataset.requiresActiveTemplate === 'true';
                if (requiresActiveTemplate && !hasActivePromptTemplate()) {
                    window.alert('활성화된 프롬프트 템플릿이 없습니다. 먼저 프롬프트를 활성화해주세요.');
                    return;
                }
                activateGenerationTab(button.dataset.tabTarget);
            });
        });
    }

    /**
     * @date 2026-04-16
     * @desc 활성 프롬프트가 없을 때 수동/예약 생성 폼 제출을 차단합니다.
     */
    function bindGenerationActionGuard() {
        const promptPanel = document.querySelector('.generation-tab-panel[data-tab-panel="prompt"]');
        if (!promptPanel) {
            return;
        }
        const guardedForms = document.querySelectorAll('[data-tab-panel="manual"] form, [data-tab-panel="schedule"] form');
        guardedForms.forEach((form) => {
            form.addEventListener('submit', (event) => {
                if (hasActivePromptTemplate()) {
                    return;
                }
                window.alert('활성화된 프롬프트 템플릿이 없습니다. 먼저 프롬프트를 활성화해주세요.');
                event.preventDefault();
            });
        });
    }

    /**
     * @date 2026-04-16
     * @desc 다른 프롬프트 템플릿 활성화 시 변경 확인 모달을 노출합니다.
     */
    function bindPromptTemplateToggleConfirm() {
        const promptPanel = document.querySelector('.generation-tab-panel[data-tab-panel="prompt"]');
        const toggleForms = document.querySelectorAll('.prompt-toggle-form');
        if (!promptPanel || toggleForms.length === 0) {
            return;
        }

        const activeTemplateName = promptPanel.dataset.activeTemplateName || '';
        toggleForms.forEach((form) => {
            form.addEventListener('submit', (event) => {
                const isTemplateActive = form.dataset.templateActive === 'true';
                if (isTemplateActive) {
                    return;
                }

                const targetTemplateName = form.dataset.templateName || '';
                const currentTemplateLabel = activeTemplateName || '없음';
                const message = '현재 활성 템플릿: ' + currentTemplateLabel + '\n'
                        + '변경 대상 템플릿: ' + targetTemplateName + '\n'
                        + '활성 템플릿을 변경하시겠습니까?';

                const isConfirmed = window.confirm(message);
                if (!isConfirmed) {
                    event.preventDefault();
                }
            });
        });
    }

    /**
     * @date 2026-04-16
     * @desc 프롬프트 템플릿 표를 단일 기준으로 정렬합니다.
     */
    function bindPromptTemplateSort() {
        const table = document.getElementById('promptTemplateTable');
        if (!table) {
            return;
        }

        const sortHeaders = table.querySelectorAll('.admin-sort-th[data-sort-key]');
        const tableBody = table.querySelector('tbody');
        if (!tableBody || sortHeaders.length === 0) {
            return;
        }

        let currentSortKey = 'id';
        let currentSortOrder = 'asc';

        /**
         * @date 2026-04-16
         * @desc 정렬 방향을 헤더 인디케이터에 반영합니다.
         */
        function updateSortIndicators() {
            sortHeaders.forEach((header) => {
                const indicator = header.querySelector('.sort-indicator');
                if (!indicator) {
                    return;
                }

                if (header.dataset.sortKey !== currentSortKey) {
                    indicator.textContent = '';
                    return;
                }
                indicator.textContent = currentSortOrder === 'asc' ? '▲' : '▼';
            });
        }

        /**
         * @date 2026-04-16
         * @desc 선택한 키 기준으로 행 목록을 정렬합니다.
         */
        function sortRows() {
            const rows = Array.from(tableBody.querySelectorAll('tr'));
            rows.sort((rowA, rowB) => {
                const valueA = getSortValue(rowA, currentSortKey);
                const valueB = getSortValue(rowB, currentSortKey);

                if (valueA < valueB) {
                    return currentSortOrder === 'asc' ? -1 : 1;
                }
                if (valueA > valueB) {
                    return currentSortOrder === 'asc' ? 1 : -1;
                }
                return 0;
            });

            rows.forEach((row) => {
                tableBody.appendChild(row);
            });
            updateSortIndicators();
        }

        sortHeaders.forEach((header) => {
            header.addEventListener('click', () => {
                const nextSortKey = header.dataset.sortKey;
                if (currentSortKey === nextSortKey) {
                    currentSortOrder = currentSortOrder === 'asc' ? 'desc' : 'asc';
                } else {
                    currentSortKey = nextSortKey;
                    currentSortOrder = 'asc';
                }
                sortRows();
            });
        });

        sortRows();
    }

    /**
     * @date 2026-04-16
     * @desc 행의 데이터셋에서 정렬 비교값을 반환합니다.
     */
    function getSortValue(row, sortKey) {
        if (sortKey === 'id') {
            return Number(row.dataset.templateId || 0);
        }
        if (sortKey === 'updatedAt') {
            return Number(row.dataset.templateUpdatedAt || 0);
        }
        return String(row.dataset.templateName || '').toLowerCase();
    }

    /**
     * @date 2026-04-16
     * @desc 생성 이력 테이블 헤더 클릭 정렬 기능을 등록합니다.
     */
    function bindGenerationHistorySort() {
        const table = document.getElementById('generationHistoryTable');
        if (!table) {
            return;
        }

        const sortHeaders = table.querySelectorAll('.admin-sort-th[data-history-sort-key]');
        const tableBody = table.querySelector('tbody');
        if (!tableBody || sortHeaders.length === 0) {
            return;
        }

        let currentSortKey = 'createdAt';
        let currentSortOrder = 'desc';

        /**
         * @date 2026-04-16
         * @desc 생성 이력 정렬 인디케이터를 갱신합니다.
         */
        function updateHistorySortIndicators() {
            sortHeaders.forEach((header) => {
                const indicator = header.querySelector('.sort-indicator');
                if (!indicator) {
                    return;
                }
                if (header.dataset.historySortKey !== currentSortKey) {
                    indicator.textContent = '';
                    return;
                }
                indicator.textContent = currentSortOrder === 'asc' ? '▲' : '▼';
            });
        }

        /**
         * @date 2026-04-16
         * @desc 선택된 기준으로 생성 이력 행을 정렬합니다.
         */
        function sortHistoryRows() {
            const rows = Array.from(tableBody.querySelectorAll('tr'))
                .filter((row) => row.dataset.historyCreatedAt);
            rows.sort((rowA, rowB) => {
                const valueA = getHistorySortValue(rowA, currentSortKey);
                const valueB = getHistorySortValue(rowB, currentSortKey);
                if (valueA < valueB) {
                    return currentSortOrder === 'asc' ? -1 : 1;
                }
                if (valueA > valueB) {
                    return currentSortOrder === 'asc' ? 1 : -1;
                }
                return 0;
            });
            rows.forEach((row) => tableBody.appendChild(row));
            updateHistorySortIndicators();
        }

        sortHeaders.forEach((header) => {
            header.addEventListener('click', () => {
                const nextSortKey = header.dataset.historySortKey;
                if (currentSortKey === nextSortKey) {
                    currentSortOrder = currentSortOrder === 'asc' ? 'desc' : 'asc';
                } else {
                    currentSortKey = nextSortKey;
                    currentSortOrder = 'asc';
                }
                sortHistoryRows();
            });
        });

        sortHistoryRows();
    }

    /**
     * @date 2026-04-16
     * @desc 생성 이력 행에서 정렬용 비교값을 반환합니다.
     */
    function getHistorySortValue(row, sortKey) {
        if (sortKey === 'createdAt') {
            return Number(row.dataset.historyCreatedAt || 0);
        }
        if (sortKey === 'targetDate') {
            return Number(row.dataset.historyTargetDate || 0);
        }
        if (sortKey === 'insertedCount') {
            return Number(row.dataset.historyInsertedCount || 0);
        }
        if (sortKey === 'createdKnowledgeId') {
            return Number(row.dataset.historyCreatedKnowledgeId || 0);
        }
        if (sortKey === 'triggerType') {
            return String(row.dataset.historyTriggerType || '').toLowerCase();
        }
        if (sortKey === 'status') {
            return String(row.dataset.historyStatus || '').toLowerCase();
        }
        return '';
    }

    /**
     * @date 2026-04-16
     * @desc 생성 이력 오류 메시지 클릭 시 상세 모달을 열고 전체 내용을 표시합니다.
     */
    function bindHistoryErrorModal() {
        const overlay = document.getElementById('historyErrorModalOverlay');
        const content = document.getElementById('historyErrorModalContent');
        const closeButton = document.getElementById('historyErrorModalClose');
        const triggers = document.querySelectorAll('.history-error-trigger');
        if (!overlay || !content || !closeButton || triggers.length === 0) {
            return;
        }

        overlay.style.display = 'none';

        /**
         * @date 2026-04-16
         * @desc 오류 상세 모달을 닫습니다.
         */
        function closeModal() {
            overlay.classList.remove('is-open');
            overlay.setAttribute('aria-hidden', 'true');
            overlay.style.display = 'none';
            document.body.style.overflow = '';
        }

        triggers.forEach((trigger) => {
            trigger.addEventListener('click', () => {
                const container = trigger.closest('.history-error-cell');
                const fullErrorInput = container ? container.querySelector('.history-error-full') : null;
                const fullErrorMessage = fullErrorInput ? fullErrorInput.value : '';
                content.textContent = fullErrorMessage || trigger.getAttribute('title') || trigger.textContent || '-';
                overlay.classList.add('is-open');
                overlay.setAttribute('aria-hidden', 'false');
                overlay.style.display = 'flex';
                document.body.style.overflow = 'hidden';
            });
        });

        closeButton.addEventListener('click', closeModal);
        overlay.addEventListener('click', (event) => {
            if (event.target === overlay) {
                closeModal();
            }
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && overlay.classList.contains('is-open')) {
                closeModal();
            }
        });
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 입력값을 새창 URL 쿼리로 인코딩합니다.
     */
    function buildComposeQueryString(targetDate, category, tone, difficulty) {
        const searchParams = new URLSearchParams();
        searchParams.set('targetDate', targetDate);
        searchParams.set('category', category);
        searchParams.set('tone', tone);
        searchParams.set('difficulty', difficulty);
        return searchParams.toString();
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 버튼 클릭 시 3개 세션 편집 새창을 엽니다.
     */
    function bindManualGenerationComposeWindow() {
        const openButton = document.getElementById('openManualGenerationComposeWindow');
        const manualForm = document.getElementById('manualGenerationForm');
        if (!openButton || !manualForm) {
            return;
        }

        openButton.addEventListener('click', () => {
            if (!hasActivePromptTemplate()) {
                window.alert('활성화된 프롬프트 템플릿이 없습니다. 먼저 프롬프트를 활성화해주세요.');
                return;
            }

            const targetDate = manualForm.querySelector('#targetDate')?.value || '';
            const category = manualForm.querySelector('#category')?.value || '';
            const tone = manualForm.querySelector('#tone')?.value || '';
            const difficulty = manualForm.querySelector('#difficulty')?.value || '';
            if (!targetDate || !category || !tone || !difficulty) {
                window.alert('대상 날짜/카테고리/톤/난이도를 모두 입력해주세요.');
                return;
            }

            const queryString = buildComposeQueryString(targetDate, category, tone, difficulty);
            const composeWindowUrl = '/admin/generation/compose?' + queryString;
            window.open(
                composeWindowUrl,
                'manual_generation_compose',
                'width=1400,height=920,menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes'
            );
        });
    }

    /**
     * @date 2026-04-17
     * @desc 좌측 메뉴 그룹(생성 관리) 아코디언 접기/펼치기 동작을 등록하고 상태를 유지합니다.
     */
    function bindSideNavAccordion() {
        const groups = document.querySelectorAll('.admin-side-nav-group[data-accordion-group="true"]');
        if (groups.length === 0) {
            return;
        }

        groups.forEach((group, index) => {
            const toggleButton = group.querySelector('.admin-side-nav-group-toggle');
            if (!toggleButton) {
                return;
            }

            const defaultOpen = group.dataset.accordionDefaultOpen === 'true';
            const storageKey = group.dataset.accordionStorageKey || ('admin-side-nav-accordion-' + index);
            let savedState = null;
            try {
                savedState = window.localStorage.getItem(storageKey);
            } catch (error) {
                savedState = null;
            }
            const isOpen = savedState === null ? defaultOpen : savedState === 'open';

            /**
             * @date 2026-04-17
             * @desc 아코디언 열린/닫힘 상태를 클래스와 접근성 속성에 반영합니다.
             */
            function applyAccordionState(open) {
                group.classList.toggle('is-collapsed', !open);
                toggleButton.setAttribute('aria-expanded', String(open));
            }

            applyAccordionState(isOpen);

            toggleButton.addEventListener('click', () => {
                const willOpen = group.classList.contains('is-collapsed');
                applyAccordionState(willOpen);
                try {
                    window.localStorage.setItem(storageKey, willOpen ? 'open' : 'collapsed');
                } catch (error) {
                    // localStorage 사용이 불가한 환경에서는 메모리 상태만 유지합니다.
                }
            });
        });
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 페이지 초기 스크립트를 실행합니다.
     */
    function initializeAdminPage() {
        bindTemplateLoadButtons();
        bindResetButton();
        bindGenerationTabs();
        bindGenerationActionGuard();
        bindPromptTemplateSaveLimitGuard();
        bindPromptTemplateToggleConfirm();
        bindPromptTemplateSort();
        bindGenerationHistorySort();
        bindHistoryErrorModal();
        bindManualGenerationComposeWindow();
        bindSideNavAccordion();
    }

    initializeAdminPage();
})();
