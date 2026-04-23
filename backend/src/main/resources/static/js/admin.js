(function () {
    /**
     * @date 2026-04-15
     * @desc 기능 설명을 처리합니다.
     */
    function bindTemplateLoadButtons() {
        const buttons = document.querySelectorAll('.load-template-btn');
        buttons.forEach((button) => {
            button.addEventListener('click', () => {
                setPromptFormValue(button.dataset);
                openPromptTemplateModal('edit');
                updatePromptTemplateSaveButtonState();
            });
        });
    }

    /**
     * @date 2026-04-15
     * @desc 기능 설명을 처리합니다.
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
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function clearPromptTemplateForm() {
        const form = document.getElementById('promptTemplateForm');
        const idField = document.getElementById('promptId');
        if (!form || !idField) {
            return;
        }
        form.reset();
        idField.value = '';
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function openPromptTemplateModal(mode) {
        const overlay = document.getElementById('promptTemplateModalOverlay');
        const title = document.getElementById('promptTemplateModalTitle');
        const saveButton = document.getElementById('savePromptTemplateButton');
        if (!overlay || !title || !saveButton) {
            return;
        }

        if (mode === 'create') {
            clearPromptTemplateForm();
            title.textContent = '프롬프트 템플릿 신규 등록';
            saveButton.textContent = 'Save';
        } else {
            title.textContent = '프롬프트 템플릿 편집';
            saveButton.textContent = 'Update';
        }

        overlay.classList.add('is-open');
        overlay.setAttribute('aria-hidden', 'false');
        overlay.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function closePromptTemplateModal() {
        const overlay = document.getElementById('promptTemplateModalOverlay');
        if (!overlay) {
            return;
        }
        overlay.classList.remove('is-open');
        overlay.setAttribute('aria-hidden', 'true');
        overlay.style.display = 'none';
        document.body.style.overflow = '';
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function bindPromptTemplateModal() {
        const overlay = document.getElementById('promptTemplateModalOverlay');
        const openCreateButton = document.getElementById('openPromptTemplateCreateModal');
        const cancelButton = document.getElementById('cancelPromptTemplateModalButton');
        if (!overlay || !openCreateButton || !cancelButton) {
            return;
        }

        overlay.style.display = 'none';
        openCreateButton.addEventListener('click', () => {
            openPromptTemplateModal('create');
            updatePromptTemplateSaveButtonState();
        });

        cancelButton.addEventListener('click', () => {
            closePromptTemplateModal();
        });

        overlay.addEventListener('click', (event) => {
            if (event.target === overlay) {
                closePromptTemplateModal();
            }
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && overlay.classList.contains('is-open')) {
                closePromptTemplateModal();
            }
        });
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function clearCrawlPresetModalForm() {
        const form = document.getElementById('crawlPresetModalForm');
        const idField = document.getElementById('crawlPresetId');
        if (!form || !idField) {
            return;
        }
        form.reset();
        idField.value = '';

        const includeWrapper = form.querySelector('.dynamic-input-wrapper[data-input-name="includeKeywords"]');
        const excludeWrapper = form.querySelector('.dynamic-input-wrapper[data-input-name="excludeKeywords"]');
        const domainWrapper = form.querySelector('.dynamic-input-wrapper[data-input-name="targetDomains"]');
        if (includeWrapper) {
            setDynamicInputValues(includeWrapper, '', '');
        }
        if (excludeWrapper) {
            setDynamicInputValues(excludeWrapper, '');
        }
        if (domainWrapper) {
            setDynamicInputValues(domainWrapper, '');
        }
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function openCrawlPresetModal(mode) {
        const overlay = document.getElementById('crawlPresetModalOverlay');
        const title = document.getElementById('crawlPresetModalTitle');
        const saveButton = document.getElementById('saveCrawlPresetButton');
        if (!overlay || !title || !saveButton) {
            return;
        }

        if (mode === 'create') {
            clearCrawlPresetModalForm();
            title.textContent = '크롤링 프리셋 신규 등록';
            saveButton.textContent = 'Save';
        } else {
            title.textContent = '크롤링 프리셋 편집';
            saveButton.textContent = 'Update';
        }

        overlay.classList.add('is-open');
        overlay.setAttribute('aria-hidden', 'false');
        overlay.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function closeCrawlPresetModal() {
        const overlay = document.getElementById('crawlPresetModalOverlay');
        if (!overlay) {
            return;
        }
        overlay.classList.remove('is-open');
        overlay.setAttribute('aria-hidden', 'true');
        overlay.style.display = 'none';
        document.body.style.overflow = '';
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function bindCrawlPresetModal() {
        const overlay = document.getElementById('crawlPresetModalOverlay');
        const openButton = document.getElementById('openCrawlPresetCreateModal');
        const cancelButton = document.getElementById('cancelCrawlPresetModalButton');
        if (!overlay || !openButton || !cancelButton) {
            return;
        }

        overlay.style.display = 'none';
        openButton.addEventListener('click', () => {
            openCrawlPresetModal('create');
        });

        cancelButton.addEventListener('click', () => {
            closeCrawlPresetModal();
        });

        overlay.addEventListener('click', (event) => {
            if (event.target === overlay) {
                closeCrawlPresetModal();
            }
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && overlay.classList.contains('is-open')) {
                closeCrawlPresetModal();
            }
        });
    }

    /**
     * @date 2026-04-16
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function bindManualGenerationPresetInputs() {
        const groups = document.querySelectorAll('[data-preset-group]');
        if (groups.length === 0) {
            return;
        }

        groups.forEach((group) => {
            const hiddenInput = document.getElementById(group.dataset.hiddenFieldId || '');
            const presetSelect = document.getElementById(group.dataset.presetSelectId || '');
            const customInput = document.getElementById(group.dataset.customInputId || '');
            if (!hiddenInput || !presetSelect || !customInput) {
                return;
            }

            const initialValue = (hiddenInput.value || '').trim();
            const hasPresetOption = Array.from(presetSelect.options).some((option) => option.value === initialValue);
            if (hasPresetOption) {
                presetSelect.value = initialValue;
                customInput.value = '';
                customInput.disabled = true;
            } else {
                presetSelect.value = '__custom__';
                customInput.value = initialValue;
                customInput.disabled = false;
            }

            /**
             * @date 2026-04-17
             * @desc 기능 설명을 처리합니다.
             */
            function syncPresetValue() {
                if (presetSelect.value === '__custom__') {
                    customInput.disabled = false;
                    hiddenInput.value = customInput.value.trim();
                    return;
                }
                customInput.disabled = true;
                hiddenInput.value = presetSelect.value;
            }

            presetSelect.addEventListener('change', () => {
                if (presetSelect.value !== '__custom__') {
                    customInput.value = '';
                }
                syncPresetValue();
            });

            customInput.addEventListener('input', () => {
                if (presetSelect.value !== '__custom__') {
                    return;
                }
                syncPresetValue();
            });

            syncPresetValue();
        });
    }

    /**
     * @date 2026-04-16
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @date 2026-04-23
     * @desc 하단 우측에 토스트 메시지를 표시합니다.
     */
    function showAdminToast(message, variant) {
        let container = document.getElementById('adminToastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'adminToastContainer';
            container.className = 'admin-toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = 'admin-toast ' + (variant || 'success');
        toast.textContent = message;
        container.appendChild(toast);

        window.setTimeout(() => {
            toast.remove();
            if (container && container.childElementCount === 0) {
                container.remove();
            }
        }, 2200);
    }

    /**
     * @date 2026-04-23
     * @desc 예약 생성 토글 상태에 따라 카드/배지/입력 영역 표시를 동기화합니다.
     */
    function bindGenerationScheduleToggleUi() {
        const card = document.getElementById('generationScheduleCard');
        const scheduleForm = document.getElementById('generationScheduleForm');
        const enabledToggle = document.getElementById('scheduleEnabledToggle');
        const allowDuplicateToggle = document.getElementById('scheduleAllowDuplicateToggle');
        const statusBadge = document.getElementById('scheduleStatusBadge');
        const editableFields = document.getElementById('scheduleEditableFields');
        const softDisabledGuide = document.getElementById('scheduleSoftDisabledGuide');
        if (!card || !scheduleForm || !enabledToggle || !allowDuplicateToggle || !statusBadge || !editableFields || !softDisabledGuide) {
            return;
        }

        const controllableFields = editableFields.querySelectorAll('input, select, textarea');

        /**
         * @date 2026-04-23
         * @desc 예약 활성 상태에 맞춰 UI 상태를 반영합니다.
         */
        function applyScheduleUiState(isActive) {
            card.classList.toggle('is-active', isActive);
            card.classList.toggle('is-inactive', !isActive);
            card.dataset.scheduleActive = String(isActive);

            statusBadge.classList.toggle('active', isActive);
            statusBadge.classList.toggle('inactive', !isActive);
            statusBadge.textContent = isActive ? '⚡ ACTIVE' : '⏸ INACTIVE';

            editableFields.classList.toggle('is-soft-disabled', !isActive);
            softDisabledGuide.hidden = isActive;
            allowDuplicateToggle.disabled = !isActive;

            controllableFields.forEach((field) => {
                field.setAttribute('aria-disabled', String(!isActive));
            });
        }

        applyScheduleUiState(enabledToggle.checked);

        enabledToggle.addEventListener('change', () => {
            const isActive = enabledToggle.checked;
            applyScheduleUiState(isActive);
            if (isActive) {
                showAdminToast('예약이 활성화되었습니다', 'success');
                return;
            }
            showAdminToast('예약이 비활성화되었습니다', 'success');
        });
    }

    /**
     * @date 2026-04-23
     * @desc 크롤링 예약 토글 상태에 따라 카드/배지/입력 영역 표시를 동기화합니다.
     */
    function bindCrawlScheduleToggleUi() {
        const card = document.getElementById('crawlScheduleCard');
        const scheduleForm = document.getElementById('crawlScheduleFormSection');
        const enabledToggle = document.getElementById('crawlScheduleEnabledToggle');
        const allowDuplicateToggle = document.getElementById('crawlScheduleAllowDuplicateToggle');
        const statusBadge = document.getElementById('crawlScheduleStatusBadge');
        const editableFields = document.getElementById('crawlScheduleEditableFields');
        const softDisabledGuide = document.getElementById('crawlScheduleSoftDisabledGuide');
        if (!card || !scheduleForm || !enabledToggle || !allowDuplicateToggle || !statusBadge || !editableFields || !softDisabledGuide) {
            return;
        }

        const controllableFields = editableFields.querySelectorAll('input, select, textarea');

        /**
         * @date 2026-04-23
         * @desc 크롤링 예약 활성 상태에 맞춰 UI 상태를 반영합니다.
         */
        function applyScheduleUiState(isActive) {
            card.classList.toggle('is-active', isActive);
            card.classList.toggle('is-inactive', !isActive);
            card.dataset.scheduleActive = String(isActive);

            statusBadge.classList.toggle('active', isActive);
            statusBadge.classList.toggle('inactive', !isActive);
            statusBadge.textContent = isActive ? '⚡ ACTIVE' : '⏸ INACTIVE';

            editableFields.classList.toggle('is-soft-disabled', !isActive);
            softDisabledGuide.hidden = isActive;
            allowDuplicateToggle.disabled = !isActive;

            controllableFields.forEach((field) => {
                field.setAttribute('aria-disabled', String(!isActive));
            });
        }

        applyScheduleUiState(enabledToggle.checked);

        enabledToggle.addEventListener('change', () => {
            const isActive = enabledToggle.checked;
            applyScheduleUiState(isActive);
            if (isActive) {
                showAdminToast('예약이 활성화되었습니다', 'success');
                return;
            }
            showAdminToast('예약이 비활성화되었습니다', 'success');
        });
    }

    /**
     * @date 2026-04-16
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
         * @desc 기능 설명을 처리합니다.
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
                indicator.textContent = currentSortOrder === 'asc' ? '^' : 'v';
            });
        }

        /**
         * @date 2026-04-16
         * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
         * @desc 기능 설명을 처리합니다.
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
                indicator.textContent = currentSortOrder === 'asc' ? '^' : 'v';
            });
        }

        /**
         * @date 2026-04-16
         * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
         * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
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
     * @desc 기능 설명을 처리합니다.
     */
    function bindDynamicInputLists() {
        const wrappers = document.querySelectorAll('.dynamic-input-wrapper[data-dynamic-list]');
        if (wrappers.length === 0) {
            return;
        }

        wrappers.forEach((wrapper) => {
            const addButton = wrapper.querySelector('[data-dynamic-add]');
            if (!addButton) {
                return;
            }

            initializeDynamicInputWrapper(wrapper);

            addButton.addEventListener('click', () => {
                const inputName = wrapper.dataset.inputName || '';
                const placeholder = wrapper.dataset.defaultPlaceholder || '';
                const maxItems = Number(wrapper.dataset.maxItems || 0);
                const currentRowCount = wrapper.querySelectorAll('.dynamic-input-row').length;
                if (maxItems > 0 && currentRowCount >= maxItems) {
                    window.alert('최대 ' + maxItems + '개까지 추가할 수 있습니다.');
                    return;
                }
                let newRow = null;
                if (inputName === 'includeKeywords') {
                    newRow = createIncludeKeywordInputRow('', 'OR', placeholder, true, wrapper.dataset.operatorName || 'includeKeywordOperators');
                } else {
                    newRow = createDynamicInputRow(inputName, '', placeholder);
                }
                wrapper.insertBefore(newRow, addButton);
                if (inputName === 'includeKeywords') {
                    refreshIncludeKeywordOperatorRows(wrapper);
                }
            });

            wrapper.addEventListener('click', (event) => {
                const removeButton = event.target.closest('[data-dynamic-remove]');
                if (!removeButton) {
                    return;
                }

                const rows = wrapper.querySelectorAll('.dynamic-input-row');
                if (rows.length <= 1) {
                    const inputField = rows[0] ? rows[0].querySelector('input') : null;
                    if (inputField) {
                        inputField.value = '';
                    }
                    return;
                }
                const targetRow = removeButton.closest('.dynamic-input-row');
                if (targetRow) {
                    targetRow.remove();
                }
                if ((wrapper.dataset.inputName || '') === 'includeKeywords') {
                    refreshIncludeKeywordOperatorRows(wrapper);
                }
            });
        });
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function createDynamicInputRow(inputName, value, placeholder) {
        const row = document.createElement('div');
        row.className = 'dynamic-input-row';

        const inputField = document.createElement('input');
        inputField.type = 'text';
        inputField.name = inputName;
        inputField.value = value || '';
        inputField.placeholder = placeholder || '';

        const removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'secondary';
        removeButton.setAttribute('data-dynamic-remove', 'true');
        removeButton.textContent = '-';

        row.appendChild(inputField);
        row.appendChild(removeButton);
        return row;
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function createIncludeKeywordInputRow(value, operator, placeholder, withOperator, operatorName) {
        const row = document.createElement('div');
        row.className = 'dynamic-input-row include-keyword-row';

        row.appendChild(createIncludeKeywordOperatorSelect(operatorName, operator));

        const inputField = document.createElement('input');
        inputField.type = 'text';
        inputField.name = 'includeKeywords';
        inputField.value = value || '';
        inputField.placeholder = placeholder || '';

        const removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'secondary';
        removeButton.setAttribute('data-dynamic-remove', 'true');
        removeButton.textContent = '-';

        row.appendChild(inputField);
        row.appendChild(removeButton);
        return row;
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function createIncludeKeywordOperatorSelect(operatorName, operatorValue) {
        const selectField = document.createElement('select');
        selectField.name = operatorName || 'includeKeywordOperators';
        selectField.className = 'include-keyword-operator-select';

        const orOption = document.createElement('option');
        orOption.value = 'OR';
        orOption.textContent = 'OR';
        selectField.appendChild(orOption);

        const andOption = document.createElement('option');
        andOption.value = 'AND';
        andOption.textContent = 'AND';
        selectField.appendChild(andOption);

        selectField.value = operatorValue === 'AND' ? 'AND' : 'OR';
        return selectField;
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function initializeDynamicInputWrapper(wrapper) {
        if ((wrapper.dataset.inputName || '') !== 'includeKeywords') {
            return;
        }
        const currentKeywordValues = Array.from(wrapper.querySelectorAll('.dynamic-input-row input[name="includeKeywords"]'))
            .map((inputField) => (inputField.value || '').trim())
            .filter((value) => value.length > 0)
            .join(',');
        const operatorCsvText = wrapper.dataset.operatorCsv || '';
        setDynamicInputValues(wrapper, currentKeywordValues, operatorCsvText);
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function setDynamicInputValues(wrapper, csvText, operatorCsvText) {
        const inputName = wrapper.dataset.inputName || '';
        const operatorName = wrapper.dataset.operatorName || 'includeKeywordOperators';
        const placeholder = wrapper.dataset.defaultPlaceholder || '';
        const maxItems = Number(wrapper.dataset.maxItems || 0);
        const addButton = wrapper.querySelector('[data-dynamic-add]');
        if (!addButton) {
            return;
        }

        wrapper.querySelectorAll('.dynamic-input-row').forEach((row) => row.remove());

        const values = (csvText || '')
            .split(',')
            .map((value) => value.trim())
            .filter((value) => value.length > 0);
        const limitedValues = maxItems > 0 ? values.slice(0, maxItems) : values;

        const operators = (operatorCsvText || '')
            .split(',')
            .map((value) => value.trim().toUpperCase())
            .filter((value) => value === 'AND' || value === 'OR');

        if (limitedValues.length === 0) {
            if (inputName === 'includeKeywords') {
                wrapper.insertBefore(createIncludeKeywordInputRow('', 'OR', placeholder, true, operatorName), addButton);
            } else {
                wrapper.insertBefore(createDynamicInputRow(inputName, '', placeholder), addButton);
            }
            return;
        }

        limitedValues.forEach((value, index) => {
            if (inputName === 'includeKeywords') {
                const operatorValue = operators[index] || 'OR';
                wrapper.insertBefore(
                    createIncludeKeywordInputRow(value, operatorValue, placeholder, true, operatorName),
                    addButton
                );
                return;
            }
            wrapper.insertBefore(createDynamicInputRow(inputName, value, placeholder), addButton);
        });
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function refreshIncludeKeywordOperatorRows(wrapper) {
        const rows = Array.from(wrapper.querySelectorAll('.dynamic-input-row'));
        const operatorName = wrapper.dataset.operatorName || 'includeKeywordOperators';
        rows.forEach((row) => {
            const existingSelect = row.querySelector('.include-keyword-operator-select');
            if (!existingSelect) {
                row.insertBefore(createIncludeKeywordOperatorSelect(operatorName, 'OR'), row.firstChild);
            } else {
                existingSelect.name = operatorName;
            }
        });
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function bindCrawlPresetApply() {
        const applyButtons = document.querySelectorAll('[data-crawl-preset-apply]');
        if (applyButtons.length === 0) {
            return;
        }

        applyButtons.forEach((button) => {
            button.addEventListener('click', () => {
                const targetFormId = button.dataset.targetFormId || '';
                const targetForm = document.getElementById(targetFormId);
                if (!targetForm) {
                    return;
                }

                const presetSelect = document.querySelector('[data-crawl-preset-select][data-target-form-id="' + targetFormId + '"]');
                if (!presetSelect || !presetSelect.value) {
                    return;
                }

                const selectedOption = presetSelect.options[presetSelect.selectedIndex];
                applyPresetToForm(targetForm, selectedOption.dataset);
            });
        });
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    /**
     * @date 2026-04-20
     * @desc 기능 설명을 처리합니다.
     */
    function bindInitialManualCrawlPreset() {
        const manualForm = document.getElementById('crawlManualForm');
        const presetSelect = document.getElementById('crawlManualPresetSelect');
        if (!manualForm || !presetSelect) {
            return;
        }

        const presetOptions = Array.from(presetSelect.options)
            .filter((option) => option.value && option.dataset.sourceUrl);
        if (presetOptions.length === 0) {
            return;
        }

        if (presetSelect.value) {
            return;
        }

        const firstPresetOption = presetOptions[0];
        presetSelect.value = firstPresetOption.value;
        applyPresetToForm(manualForm, firstPresetOption.dataset);
    }
    function applyPresetToForm(targetForm, dataset) {
        const sourceNameField = targetForm.querySelector('input[name="sourceName"]');
        const sourceUrlField = targetForm.querySelector('input[name="sourceUrl"]');
        const maxArticlesField = targetForm.querySelector('input[name="maxArticles"]');
        const keywordMatchTypeField = targetForm.querySelector('[name="keywordMatchType"]');
        const connectTimeoutField = targetForm.querySelector('input[name="connectTimeoutSeconds"]');
        const readTimeoutField = targetForm.querySelector('input[name="readTimeoutSeconds"]');
        const retryCountField = targetForm.querySelector('input[name="retryCount"]');

        if (sourceNameField) {
            sourceNameField.value = dataset.sourceName || '';
        }
        if (sourceUrlField) {
            sourceUrlField.value = dataset.sourceUrl || '';
        }
        if (maxArticlesField) {
            maxArticlesField.value = dataset.maxArticles || '';
        }
        if (keywordMatchTypeField) {
            keywordMatchTypeField.value = dataset.keywordMatchType || 'OR';
        }
        if (connectTimeoutField) {
            connectTimeoutField.value = dataset.connectTimeoutSeconds || '';
        }
        if (readTimeoutField) {
            readTimeoutField.value = dataset.readTimeoutSeconds || '';
        }
        if (retryCountField) {
            retryCountField.value = dataset.retryCount || '';
        }

        const keywordWrapper = targetForm.querySelector('.dynamic-input-wrapper[data-input-name="includeKeywords"]');
        if (keywordWrapper) {
            setDynamicInputValues(keywordWrapper, dataset.includeKeywords || '', dataset.includeKeywordOperators || '');
        }

        const excludeKeywordWrapper = targetForm.querySelector('.dynamic-input-wrapper[data-input-name="excludeKeywords"]');
        if (excludeKeywordWrapper) {
            setDynamicInputValues(excludeKeywordWrapper, dataset.excludeKeywords || '');
        }

        const domainWrapper = targetForm.querySelector('.dynamic-input-wrapper[data-input-name="targetDomains"]');
        if (domainWrapper) {
            setDynamicInputValues(domainWrapper, dataset.targetDomains || '');
        }
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function bindCrawlPresetLoad() {
        const loadButtons = document.querySelectorAll('[data-crawl-preset-edit]');
        if (loadButtons.length === 0) {
            return;
        }

        loadButtons.forEach((button) => {
            button.addEventListener('click', () => {
                const targetForm = document.getElementById('crawlPresetModalForm');
                if (!targetForm) {
                    return;
                }

                const presetIdField = targetForm.querySelector('input[name="presetId"]');
                const presetNameField = targetForm.querySelector('input[name="presetName"]');
                if (presetIdField) {
                    presetIdField.value = button.dataset.presetId || '';
                }
                if (presetNameField) {
                    presetNameField.value = button.dataset.presetName || '';
                }

                applyPresetToForm(targetForm, button.dataset);
                openCrawlPresetModal('edit');
            });
        });
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function bindCrawlPreviewModal() {
        const manualForm = document.getElementById('crawlManualForm');
        const overlay = document.getElementById('crawlPreviewModalOverlay');
        const cancelButton = document.getElementById('cancelCrawlPreviewModalButton');
        const confirmButton = document.getElementById('confirmCrawlPreviewRunButton');
        const summaryElement = document.getElementById('crawlPreviewSummary');
        const tableBody = document.getElementById('crawlPreviewTableBody');
        const runProgressOverlay = document.getElementById('crawlRunProgressModalOverlay');
        const runProgressElapsedText = document.getElementById('crawlRunProgressElapsedText');
        if (!manualForm || !overlay || !cancelButton || !confirmButton || !summaryElement || !tableBody) {
            return;
        }

        overlay.style.display = 'none';
        if (runProgressOverlay) {
            runProgressOverlay.style.display = 'none';
        }

        let previewTimerId = null;
        let previewElapsedSeconds = 0;
        let isRunConfirmed = false;
        let runProgressTimerId = null;
        let runProgressElapsedSeconds = 0;

        /**
         * @date 2026-04-17
         * @desc 기능 설명을 처리합니다.
         */
        function openModal() {
            overlay.classList.add('is-open');
            overlay.setAttribute('aria-hidden', 'false');
            overlay.style.display = 'flex';
            document.body.style.overflow = 'hidden';
        }

        /**
         * @date 2026-04-17
         * @desc 기능 설명을 처리합니다.
         */
        function closeModal() {
            overlay.classList.remove('is-open');
            overlay.setAttribute('aria-hidden', 'true');
            overlay.style.display = 'none';
            document.body.style.overflow = '';
        }

        /**
         * @date 2026-04-20
         * @desc 기능 설명을 처리합니다.
         */
        function startPreviewTimer() {
            previewElapsedSeconds = 0;
            previewTimerId = window.setInterval(() => {
                previewElapsedSeconds += 1;
                summaryElement.textContent = '미리보기 크롤링 진행 중.. ' + previewElapsedSeconds + '초';
            }, 1000);
        }

        /**
         * @date 2026-04-20
         * @desc 기능 설명을 처리합니다.
         */
        function stopPreviewTimer() {
            if (!previewTimerId) {
                return;
            }
            window.clearInterval(previewTimerId);
            previewTimerId = null;
        }

        /**
         * @date 2026-04-17
         * @desc 기능 설명을 처리합니다.
         */
        function renderPreviewItems(previewItems) {
            tableBody.innerHTML = '';
            if (!previewItems || previewItems.length === 0) {
                const emptyRow = document.createElement('tr');
                const emptyCell = document.createElement('td');
                emptyCell.colSpan = 2;
                emptyCell.textContent = '조건에 맞는 결과가 없습니다.';
                emptyRow.appendChild(emptyCell);
                tableBody.appendChild(emptyRow);
                return;
            }

            previewItems.forEach((item) => {
                const row = document.createElement('tr');
                const titleCell = document.createElement('td');
                const urlCell = document.createElement('td');
                titleCell.textContent = item.title || '제목 없음';

                if (item.url) {
                    const link = document.createElement('a');
                    link.href = item.url;
                    link.target = '_blank';
                    link.rel = 'noopener noreferrer';
                    link.textContent = item.url;
                    urlCell.appendChild(link);
                } else {
                    urlCell.textContent = '-';
                }

                row.appendChild(titleCell);
                row.appendChild(urlCell);
                tableBody.appendChild(row);
            });
        }

        /**
         * @date 2026-04-17
         * @desc 기능 설명을 처리합니다.
         */
        function buildCrawlPreviewRequest() {
            const formData = new FormData(manualForm);
            const requestBody = {
                targetDate: (formData.get('targetDate') || '').toString(),
                sourceName: (formData.get('sourceName') || '').toString(),
                sourceUrl: (formData.get('sourceUrl') || '').toString(),
                maxArticles: Number(formData.get('maxArticles') || 0),
                keywordMatchType: (formData.get('keywordMatchType') || 'OR').toString(),
                includeKeywords: collectFormArrayValues(formData, 'includeKeywords'),
                includeKeywordOperators: collectFormArrayValues(formData, 'includeKeywordOperators'),
                excludeKeywords: collectFormArrayValues(formData, 'excludeKeywords'),
                targetDomains: collectFormArrayValues(formData, 'targetDomains'),
                connectTimeoutSeconds: Number(formData.get('connectTimeoutSeconds') || 0),
                readTimeoutSeconds: Number(formData.get('readTimeoutSeconds') || 0),
                retryCount: Number(formData.get('retryCount') || 0)
            };
            return requestBody;
        }

        /**
         * @date 2026-04-20
         * @desc 기능 설명을 처리합니다.
         */
        function openRunProgressOverlay() {
            if (!runProgressOverlay || !runProgressElapsedText) {
                return;
            }
            runProgressElapsedSeconds = 0;
            runProgressElapsedText.textContent = '진행 시간: 0초';
            runProgressOverlay.classList.add('is-open');
            runProgressOverlay.setAttribute('aria-hidden', 'false');
            runProgressOverlay.style.display = 'flex';
            document.body.style.overflow = 'hidden';
            runProgressTimerId = window.setInterval(() => {
                runProgressElapsedSeconds += 1;
                runProgressElapsedText.textContent = '진행 시간: ' + runProgressElapsedSeconds + '초';
            }, 1000);
        }

        /**
         * @date 2026-04-20
         * @desc 기능 설명을 처리합니다.
         */
        async function openPreviewByFormState() {
            if (!manualForm.reportValidity()) {
                return;
            }

            summaryElement.textContent = '미리보기 크롤링 요청을 시작합니다.';
            renderPreviewItems([]);
            openModal();
            startPreviewTimer();

            const csrfTokenField = manualForm.querySelector('input[name="_csrf"]');
            const requestHeaders = {
                'Content-Type': 'application/json'
            };
            if (csrfTokenField && csrfTokenField.value) {
                requestHeaders['X-CSRF-TOKEN'] = csrfTokenField.value;
            }

            try {
                const response = await window.fetch('/admin/crawling/preview', {
                    method: 'POST',
                    headers: requestHeaders,
                    body: JSON.stringify(buildCrawlPreviewRequest())
                });
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }

                const payload = await response.json();
                if (!payload.success) {
                    summaryElement.textContent = payload.message || '미리보기 생성에 실패했습니다.';
                    renderPreviewItems([]);
                    stopPreviewTimer();
                    return;
                }

                summaryElement.textContent = '수집 ' + (payload.collectedCount || 0)
                    + '건/ 조건 일치 ' + (payload.filteredCount || 0) + '건/ 소요 ' + previewElapsedSeconds + '초';
                renderPreviewItems(payload.previewItems || []);
                stopPreviewTimer();
            } catch (error) {
                summaryElement.textContent = '미리보기 조회 중 오류가 발생했습니다.';
                renderPreviewItems([]);
                stopPreviewTimer();
            }
        }

        manualForm.addEventListener('submit', async (event) => {
            if (isRunConfirmed) {
                openRunProgressOverlay();
                return;
            }
            event.preventDefault();
            await openPreviewByFormState();
        });

        cancelButton.addEventListener('click', closeModal);
        confirmButton.addEventListener('click', () => {
            closeModal();
            isRunConfirmed = true;
            manualForm.requestSubmit();
        });
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
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
     */
    function collectFormArrayValues(formData, fieldName) {
        return formData.getAll(fieldName)
            .map((value) => String(value || '').trim())
            .filter((value) => value.length > 0);
    }

    /**
     * @date 2026-04-17
     * @desc 기능 설명을 처리합니다.
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
             * @desc 기능 설명을 처리합니다.
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
                    // localStorage를 사용할 수 없는 환경에서는 메모리 상태만 유지합니다.
                }
            });
        });
    }

    /**
     * @date 2026-04-15
     * @desc 기능 설명을 처리합니다.
     */
    function initializeAdminPage() {
        bindTemplateLoadButtons();
        bindPromptTemplateModal();
        bindCrawlPresetModal();
        bindGenerationTabs();
        bindGenerationScheduleToggleUi();
        bindCrawlScheduleToggleUi();
        bindGenerationActionGuard();
        bindPromptTemplateSaveLimitGuard();
        bindPromptTemplateToggleConfirm();
        bindPromptTemplateSort();
        bindGenerationHistorySort();
        bindHistoryErrorModal();
        bindManualGenerationPresetInputs();
        bindManualGenerationComposeWindow();
        bindDynamicInputLists();
        bindCrawlPresetApply();
        bindInitialManualCrawlPreset();
        bindCrawlPresetLoad();
        bindCrawlPreviewModal();
        bindSideNavAccordion();
    }

    initializeAdminPage();
})();

