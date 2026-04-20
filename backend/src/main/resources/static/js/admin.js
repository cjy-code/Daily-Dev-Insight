(function () {
    /**
     * @date 2026-04-15
     * @desc ???ロ깵???筌뤾쑴異??뺢퀗??????繹?筌? ?繹먮굞夷??紐껊퉵??
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
     * @desc ???ロ깵????⑥щ턄??⑥ヂ??怨쀬Ŧ ?筌뤾쑴異????띠룆????筌뤿굝???紐껊퉵??
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
     * @desc ?熬곣뫅??熬곥굥諭????놁졑 ???띠룆????リ옇?????⑤객臾뜹슖??貫?껆뵳??됀?????덈펲.
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
     * @desc ?熬곣뫅??熬곥굥諭????ロ깵?????놁졑/??瑜곸젧 嶺뚮ㅄ維??????굿???類쏄콬/?뺢퀗?????⑤객臾???띠룄????紐껊퉵??
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
            title.textContent = '?熬곣뫅??熬곥굥諭????ロ깵???怨뺣뼺?';
            saveButton.textContent = '저장';
        } else {
            title.textContent = '?熬곣뫅??熬곥굥諭????ロ깵????瑜곸젧';
            saveButton.textContent = '??瑜곸젧';
        }

        overlay.classList.add('is-open');
        overlay.setAttribute('aria-hidden', 'false');
        overlay.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    /**
     * @date 2026-04-17
     * @desc ?熬곣뫅??熬곥굥諭????ロ깵?????놁졑/??瑜곸젧 嶺뚮ㅄ維??????띉???븐뻼????⑤객臾???곌랜踰???紐껊퉵??
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
     * @desc ?熬곣뫅??熬곥굥諭????ロ깵?????놁졑/??瑜곸젧 嶺뚮ㅄ維?????⒱뵛/???뗢뵛 ???繹?筌? ?繹먮굞夷??紐껊퉵??
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
     * @desc ??夷뚨춯??브퀗?쀦뤃??熬곣뱿遊??嶺뚮ㅄ維?????놁졑?띠룆????貫?껆뵳??됀?????덈펲.
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
     * @desc ??夷뚨춯??브퀗?쀦뤃??熬곣뱿遊??嶺뚮ㅄ維??????굿???類쏄콬/?뺢퀗?????⑤객臾???띠룄????紐껊퉵??
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
            title.textContent = '?브퀗?쀦뤃??熬곣뱿遊???怨뺣뼺?';
            saveButton.textContent = '저장';
        } else {
            title.textContent = '?브퀗?쀦뤃??熬곣뱿遊????瑜곸젧';
            saveButton.textContent = '??瑜곸젧';
        }

        overlay.classList.add('is-open');
        overlay.setAttribute('aria-hidden', 'false');
        overlay.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    /**
     * @date 2026-04-17
     * @desc ??夷뚨춯??브퀗?쀦뤃??熬곣뱿遊??嶺뚮ㅄ維??????띉???븐뻼????⑤객臾???곌랜踰???紐껊퉵??
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
     * @desc ??夷뚨춯??브퀗?쀦뤃??熬곣뱿遊??嶺뚮ㅄ維?????⒱뵛/???뗢뵛 ???繹?筌? ?繹먮굞夷??紐껊퉵??
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
     * @desc ???ロ깵???띠룇裕?????ル┰????⑤벡逾??????뺢퀗?????戮?뎽 ??⑤객臾???띠룄????紐껊퉵??
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
        saveButton.title = shouldDisable ? '???ロ깵?源놁뒭? 嶺뚣끉裕? 10?띠룇裕??먯?? ?繹먮굞夷???????곕????덈펲.' : '';
    }

    /**
     * @date 2026-04-16
     * @desc ???ロ깵???????????놁졑 ??⑤객臾??곌떠??????????뺢퀗?????⑤객臾???띠룄????紐껊퉵??
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
     * @desc ??濡レ쭢 ??諛댁뎽???????諭?嶺뚯쉳?????놁졑 ?브퀗?ч뜮? ?띠룆???hidden ???놁졑?띠룆????뿉????욋뵛??됀?????덈펲.
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
             * @desc ??ルㅎ臾???⑤객臾????⑤벡逾?hidden/custom ???놁졑?띠룆????띠룄????紐껊퉵??
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
     * @desc URL ??怨룸뻣???????諛댁뎽??㉱??????? ???堉??紐껊퉵??
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
     * @desc ?熬곣뫗????戮?뎽 ?熬곣뫅??熬곥굥諭??브퀡?????????꾩룇瑗???紐껊퉵??
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
     * @desc 嶺뚯솘??筌먐삳┰ ??諛댁뎽??㉱????????戮?뎽??됀??????濡?룫嶺뚯솘? ???븐꽢????節띾룯???덈펲.
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
     * @desc ??戮?뎽 ?熬곣뫅??熬곥굥諭쒏뤆?쎛 ??怨몃굵 ????濡レ쭢/???고뒎 ???뺢퀗?????⑤객臾???띠룄????紐껊퉵??
     */
    function updateGenerationTabAccessState() {
        const tabButtons = document.querySelectorAll('.admin-tab-btn[data-requires-active-template="true"]');
        const canUseGenerationTabs = hasActivePromptTemplate();
        tabButtons.forEach((button) => {
            button.classList.toggle('disabled', !canUseGenerationTabs);
            button.setAttribute('aria-disabled', String(!canUseGenerationTabs));
            button.title = canUseGenerationTabs ? '' : '??戮?뎽 ?熬곣뫅??熬곥굥諭????ロ깵?源놁뒭????怨룹꽑 ?????????怨룸????덈펲.';
        });
    }

    /**
     * @date 2026-04-16
     * @desc ??諛댁뎽??㉱?????뺢퀗???????????繹?筌? ?繹먮굞夷??紐껊퉵??
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
                    window.alert('??戮?뎽??븐뼔彛??熬곣뫅??熬곥굥諭????ロ깵?源놁뒭????怨룸????덈펲. ?誘る닔? ?熬곣뫅??熬곥굥諭????戮?뎽??됀?????源껋돪??');
                    return;
                }
                activateGenerationTab(button.dataset.tabTarget);
            });
        });
    }

    /**
     * @date 2026-04-16
     * @desc ??戮?뎽 ?熬곣뫅??熬곥굥諭쒏뤆?쎛 ??怨몃굵 ????濡レ쭢/???고뒎 ??諛댁뎽 ????戮깅??嶺뚢뼰維???紐껊퉵??
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
                window.alert('??戮?뎽??븐뼔彛??熬곣뫅??熬곥굥諭????ロ깵?源놁뒭????怨룸????덈펲. ?誘る닔? ?熬곣뫅??熬곥굥諭????戮?뎽??됀?????源껋돪??');
                event.preventDefault();
            });
        });
    }

    /**
     * @date 2026-04-16
     * @desc ???섎??熬곣뫅??熬곥굥諭????ロ깵????戮?뎽?????곌떠????筌먦끉逾?嶺뚮ㅄ維????筌뤾쑵???紐껊퉵??
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
                const currentTemplateLabel = activeTemplateName || '??怨몃쾳';
                const message = '?熬곣뫗????戮?뎽 ???ロ깵?? ' + currentTemplateLabel + '\n'
                        + '?곌떠??????????ロ깵?? ' + targetTemplateName + '\n'
                        + '??戮?뎽 ???ロ깵?源놁뒭???곌떠??롪퍔????蹂?뱼???鍮띸뭐?';

                const isConfirmed = window.confirm(message);
                if (!isConfirmed) {
                    event.preventDefault();
                }
            });
        });
    }

    /**
     * @date 2026-04-16
     * @desc ?熬곣뫅??熬곥굥諭????ロ깵????? ??關逾??リ옇????怨쀬Ŧ ?筌먲퐣議??紐껊퉵??
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
         * @desc ?筌먲퐣議??꾩렮維싧젆?????녹맠 ?筌뤾퍓???댟??袁㏃댉???꾩룇瑗???紐껊퉵??
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
                indicator.textContent = currentSortOrder === 'asc' ? '↑' : '↓';
            });
        }

        /**
         * @date 2026-04-16
         * @desc ??ルㅎ臾?????リ옇????怨쀬Ŧ ??嶺뚮ㅄ維뽨빳???筌먲퐣議??紐껊퉵??
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
     * @desc ??源녿꺄 ??⑥щ턄??⑥ヂ??????筌먲퐣議??????묒쾸沃섅굦諭??꾩룇瑗???紐껊퉵??
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
     * @desc ??諛댁뎽 ????????逾?????녹맠 ??????筌먲퐣議??リ옇?????繹먮굞夷??紐껊퉵??
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
         * @desc ??諛댁뎽 ??????筌먲퐣議??筌뤾퍓???댟??袁㏃댉???띠룄????紐껊퉵??
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
                indicator.textContent = currentSortOrder === 'asc' ? '↑' : '↓';
            });
        }

        /**
         * @date 2026-04-16
         * @desc ??ルㅎ臾???リ옇????怨쀬Ŧ ??諛댁뎽 ???????源녿굵 ?筌먲퐣議??紐껊퉵??
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
     * @desc ??諛댁뎽 ???????源낇뱺???筌먲퐣議???????묒쾸沃섅굦諭??꾩룇瑗???紐껊퉵??
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
     * @desc ??諛댁뎽 ????????댁쾼 嶺뚮∥???낆?? ?????????⑤㈇??嶺뚮ㅄ維??????굿??熬곣뫕????怨몃뮔????戮?뻣??紐껊퉵??
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
         * @desc ???댁쾼 ??⑤㈇??嶺뚮ㅄ維??????裕???덈펲.
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
     * @desc ??濡レ쭢 ??諛댁뎽 ???놁졑?띠룆???????갗 URL ?臾믩닑?怨レ뿉??筌뤾쑵留??釉????덈펲.
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
     * @desc ??濡レ쭢 ??諛댁뎽 ?뺢퀗??????????3???筌뤾쑬???筌뤾쑴異?????갗?????덈퉵??
     */
    function bindManualGenerationComposeWindow() {
        const openButton = document.getElementById('openManualGenerationComposeWindow');
        const manualForm = document.getElementById('manualGenerationForm');
        if (!openButton || !manualForm) {
            return;
        }

        openButton.addEventListener('click', () => {
            if (!hasActivePromptTemplate()) {
                window.alert('??戮?뎽??븐뼔彛??熬곣뫅??熬곥굥諭????ロ깵?源놁뒭????怨룸????덈펲. ?誘る닔? ?熬곣뫅??熬곥굥諭????戮?뎽??됀?????源껋돪??');
                return;
            }

            const targetDate = manualForm.querySelector('#targetDate')?.value || '';
            const category = manualForm.querySelector('#category')?.value || '';
            const tone = manualForm.querySelector('#tone')?.value || '';
            const difficulty = manualForm.querySelector('#difficulty')?.value || '';
            if (!targetDate || !category || !tone || !difficulty) {
                window.alert('??????ル‘?/?곸궠??誘ㅒ?μ쪚??????戮곕턄?熬? 嶺뚮ㅄ維筌????놁졑??怨삵룖?筌뤾쑴??');
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
     * @desc ???긱돡 ???놁졑(+/-) UI???貫?껆뵳??됀???????됱쓤 ???怨뺣뼺?/???????繹?筌? ?繹먮굞夷??紐껊퉵??
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
                    window.alert('嶺뚣끉裕? ' + maxItems + '?띠룇裕??먯?? ?怨뺣뼺????????곕????덈펲.');
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
     * @desc ???긱돡 ???놁졑 ??DOM ?筌뤾퍓援????諛댁뎽??紐껊퉵??
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
     * @desc ???????깅쐳?????놁졑 ??源녿굵 ??諛댁뎽???겶??熬곣뫗??????レ뒩????⑥ろ뀰??AND/OR) ?????諭????節띾쐾 ??뚮봽???紐껊퉵??
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
     * @desc ???????깅쐳????源낇뱺?????????⑥ろ뀰??AND/OR) ?????諭??筌뤾퍓援????諛댁뎽??紐껊퉵??
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
     * @desc ???긱돡 ???놁졑 ??臾먯뱺???貫?껆뵳?DOM ?띠룆???????꽑 ???????깅쐳????⑥ろ뀰??UI??????繹먮봾????덈펲.
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
     * @desc ?熬곣뫀堉롧뛾?녿즵? CSV ??쒖굣???怨몃굵 ???긱돡 ???놁졑 ??源녿さ???꾩룇瑗???紐껊퉵??
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
     * @desc ???????깅쐳??????戮?맋??嶺뚮씮???1??? ??⑥ろ뀰????怨몃턄, 2???????レ뒩????⑥ろ뀰???????諭???꾩룄????紐껊퉵??
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
     * @desc ?熬곣뱿遊????ルㅎ臾멩뤆??????濡レ쭢/???고뒎 ??源낇뱺 ??⑤챷???濡ル츎 ?뺢퀗??????繹?筌? ?繹먮굞夷??紐껊퉵??
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
     * @desc ?熬곣뱿遊????⑥щ턄??⑥ヂ??띠룆????????????놁졑?띠룆????뿉??꾩룇瑗???紐껊퉵??
     */
    /**
     * @date 2026-04-20
     * @desc 수동 실행 탭 진입 시 프리셋이 존재하면 첫 번째 프리셋을 자동 바인딩합니다.
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
     * @desc ?熬곣뱿遊??嶺뚮ㅄ維뽨빳????瑜곸젧 ?釉띾쐞???釉띯뵛 ?뺢퀗??????繹?筌? ?繹먮굞夷??紐껊퉵??
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
     * @desc ??濡レ쭢 ??夷뚨춯?亦껋꼶梨?怨?돦??⒱뵛 嶺뚮ㅄ維?????⒱뵛/???뗢뵛/???덈뺄 ???繹?筌? ?繹먮굞夷??紐껊퉵??
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
         * @desc ??쨌筌?沃섎챶?곮퉪?용┛ 筌뤴뫀?????욱??臾롫젏???怨밴묶??揶쏄퉮???몃빍??
         */
        function openModal() {
            overlay.classList.add('is-open');
            overlay.setAttribute('aria-hidden', 'false');
            overlay.style.display = 'flex';
            document.body.style.overflow = 'hidden';
        }

        /**
         * @date 2026-04-17
         * @desc ??쨌筌?沃섎챶?곮퉪?용┛ 筌뤴뫀?????ろ??遺얇늺 ?怨밴묶??癰귣벊???몃빍??
         */
        function closeModal() {
            overlay.classList.remove('is-open');
            overlay.setAttribute('aria-hidden', 'true');
            overlay.style.display = 'none';
            document.body.style.overflow = '';
        }

        /**
         * @date 2026-04-20
         * @desc 沃섎챶?곮퉪?용┛ ??쎈뻬 野껋럡????볦퍢??雅뚯눊由?怨몄몵嚥?揶쏄퉮???몃빍??
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
         * @desc 沃섎챶?곮퉪?용┛ ??쎈뻬 野껋럡????볦퍢 ?????㎫몴??ル굝利??몃빍??
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
         * @desc 沃섎챶?곮퉪?용┛ ?臾먮뼗 野껉퀗?든몴????뵠????뺛걠/URL)嚥????쐭筌띻낱鍮??덈뼄.
         */
        function renderPreviewItems(previewItems) {
            tableBody.innerHTML = '';
            if (!previewItems || previewItems.length === 0) {
                const emptyRow = document.createElement('tr');
                const emptyCell = document.createElement('td');
                emptyCell.colSpan = 2;
                emptyCell.textContent = '鈺곌퀗援??筌띿쉶??野껉퀗?드첎? ??곷뮸??덈뼄.';
                emptyRow.appendChild(emptyCell);
                tableBody.appendChild(emptyRow);
                return;
            }

            previewItems.forEach((item) => {
                const row = document.createElement('tr');
                const titleCell = document.createElement('td');
                const urlCell = document.createElement('td');
                titleCell.textContent = item.title || '??뺛걠 ??곸벉';

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
         * @desc ??롫짗 ??쨌筌?????낆젾揶쏅???沃섎챶?곮퉪?용┛ API ?遺욧퍕 獄쏅뗀逾?揶쏆빘猿쒏에?癰궰??묐???덈뼄.
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
         * @desc ??뺤쒔 ??????쎈뻬 ?遺욧퍕 餓?野껋럡????볦퍢????뽯뻻??롫뮉 ??살쒔??됱뵠????덈빍??
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
         * @desc ?袁⑹삺 ???怨밴묶??疫꿸퀣???곗쨮 沃섎챶?곮퉪?용┛ API???紐꾪뀱??랁?野껉퀗?든몴?筌뤴뫀?????뽯뻻??몃빍??
         */
        async function openPreviewByFormState() {
            if (!manualForm.reportValidity()) {
                return;
            }

            summaryElement.textContent = '沃섎챶?곮퉪?용┛ ??쨌筌??遺욧퍕????뽰삂??몃빍??';
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
                    summaryElement.textContent = payload.message || '沃섎챶?곮퉪?용┛ ??밴쉐????쎈솭??됰뮸??덈뼄.';
                    renderPreviewItems([]);
                    stopPreviewTimer();
                    return;
                }

                summaryElement.textContent = '??륁춿 ' + (payload.collectedCount || 0)
                    + '건/ 조건 일치 ' + (payload.filteredCount || 0) + '건/ 소요 ' + previewElapsedSeconds + '초';
                renderPreviewItems(payload.previewItems || []);
                stopPreviewTimer();
            } catch (error) {
                summaryElement.textContent = '沃섎챶?곮퉪?용┛ 鈺곌퀬??餓???살첒揶쎛 獄쏆뮇源??됰뮸??덈뼄.';
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
     * @desc FormData ??쇱㉦ ??낆젾 ?袁⑤굡?癒?퐣 ?⑤벉媛???揶쏅?????볤탢??獄쏄퀣肉????밴쉐??몃빍??
     */
    function collectFormArrayValues(formData, fieldName) {
        return formData.getAll(fieldName)
            .map((value) => String(value || '').trim())
            .filter((value) => value.length > 0);
    }

    /**
     * @date 2026-04-17
     * @desc ??レ뒩??嶺뚮∥????잙갭梨띄쳥???諛댁뎽 ??㉱?? ?熬곣뫚留??븐슜????얜∥????源딅뭵?????됱굚???繹먮굞夷???겶???⑤객臾???????紐껊퉵??
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
             * @desc ?熬곣뫚留??븐슜??????????????⑤객臾????????? ??얜∥??????㏃뎽???꾩룇瑗???紐껊퉵??
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
                    // localStorage ??????釉띾쐝??????삵렱??????嶺뚮∥???꾨뎨???⑤객臾띄춯??????紐껊퉵??
                }
            });
        });
    }

    /**
     * @date 2026-04-15
     * @desc ??㉱?洹먮봿????瑜곷턄嶺뚯솘? ?貫?껆뵳????꾩씩?源??猿녿ご????덈뺄??紐껊퉵??
     */
    function initializeAdminPage() {
        bindTemplateLoadButtons();
        bindPromptTemplateModal();
        bindCrawlPresetModal();
        bindGenerationTabs();
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

