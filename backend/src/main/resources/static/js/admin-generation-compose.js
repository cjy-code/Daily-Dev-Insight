(function () {
    /**
     * @date 2026-04-16
     * @desc CSRF 메타 태그에서 헤더 이름과 토큰을 읽습니다.
     */
    function getCsrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
        const headerName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
        if (!token) {
            return {};
        }
        return { [headerName]: token };
    }

    /**
     * @date 2026-04-16
     * @desc 현재 새창의 대상 생성 조건을 dataset에서 읽어옵니다.
     */
    function getGenerationContext() {
        const shell = document.querySelector('.admin-compose-shell');
        if (!shell) {
            return null;
        }
        return {
            targetDate: shell.dataset.targetDate || '',
            category: shell.dataset.category || '',
            tone: shell.dataset.tone || '',
            difficulty: shell.dataset.difficulty || ''
        };
    }

    /**
     * @date 2026-04-16
     * @desc 새창 로딩 오버레이를 열고 상태 문구를 갱신합니다.
     */
    function openComposeLoading(message) {
        const loadingOverlay = document.getElementById('composeLoadingOverlay');
        const loadingText = document.getElementById('composeLoadingText');
        if (!loadingOverlay || !loadingText) {
            return;
        }
        loadingText.textContent = message || '처리 중입니다...';
        loadingOverlay.hidden = false;
    }

    /**
     * @date 2026-04-16
     * @desc 새창 로딩 오버레이를 닫습니다.
     */
    function closeComposeLoading() {
        const loadingOverlay = document.getElementById('composeLoadingOverlay');
        if (!loadingOverlay) {
            return;
        }
        loadingOverlay.hidden = true;
    }

    /**
     * @date 2026-04-16
     * @desc 이전 결과 입력 영역의 활성/비활성 상태를 전환합니다.
     */
    function setPreviousSectionEnabled(enabled) {
        const previousSection = document.getElementById('composePreviousSection');
        const previousFields = [
            document.getElementById('composePreviousTitle'),
            document.getElementById('composePreviousSummary'),
            document.getElementById('composePreviousDetail')
        ];
        const statusText = document.getElementById('composePreviousStatusText');
        if (!previousSection || !statusText) {
            return;
        }

        previousSection.classList.toggle('disabled', !enabled);
        previousFields.forEach((field) => {
            if (!field) {
                return;
            }
            field.disabled = !enabled;
        });

        statusText.textContent = enabled
            ? '같은 날짜의 이전 결과가 있어 비교할 수 있습니다.'
            : '같은 날짜의 이전 결과가 없어 비활성화 상태입니다.';
    }

    /**
     * @date 2026-04-16
     * @desc LLM 미리보기 결과를 화면에 반영합니다.
     */
    function applyPreviewResult(previewResponse) {
        const titleField = document.getElementById('composeGeneratedTitle');
        const summaryField = document.getElementById('composeGeneratedSummary');
        const detailField = document.getElementById('composeGeneratedDetail');
        const previousTitleField = document.getElementById('composePreviousTitle');
        const previousSummaryField = document.getElementById('composePreviousSummary');
        const previousDetailField = document.getElementById('composePreviousDetail');

        if (!titleField || !summaryField || !detailField || !previousTitleField || !previousSummaryField || !previousDetailField) {
            return;
        }

        titleField.value = previewResponse.generatedTitle || '';
        summaryField.value = previewResponse.generatedSummary || '';
        detailField.value = previewResponse.generatedDetail || '';

        if (previewResponse.hasPreviousResult) {
            previousTitleField.value = previewResponse.previousTitle || '';
            previousSummaryField.value = previewResponse.previousSummary || '';
            previousDetailField.value = previewResponse.previousDetail || '';
            setPreviousSectionEnabled(true);
            return;
        }

        previousTitleField.value = '';
        previousSummaryField.value = '';
        previousDetailField.value = '';
        setPreviousSectionEnabled(false);
    }

    /**
     * @date 2026-04-16
     * @desc 서버 미리보기 API를 호출해 LLM 결과를 생성합니다.
     */
    async function requestPreviewGeneration() {
        const promptField = document.getElementById('composePromptContent');
        const statusField = document.getElementById('composePreviewStatus');
        const saveButton = document.getElementById('composeSaveButton');
        const generationContext = getGenerationContext();
        if (!promptField || !statusField || !saveButton || !generationContext) {
            return;
        }

        const payload = {
            targetDate: generationContext.targetDate,
            category: generationContext.category,
            tone: generationContext.tone,
            difficulty: generationContext.difficulty,
            promptContent: promptField.value
        };

        statusField.textContent = 'LLM 결과를 생성하는 중입니다...';
        saveButton.disabled = true;
        openComposeLoading('LLM 결과를 생성하는 중입니다...');

        try {
            const response = await fetch('/admin/generate/preview', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getCsrfHeaders()
                },
                body: JSON.stringify(payload)
            });
            const data = await response.json();
            if (!data.success) {
                const codeLabel = data.errorCode ? '[' + data.errorCode + '] ' : '';
                statusField.textContent = codeLabel + (data.message || 'LLM 결과 생성에 실패했습니다.');
                return;
            }

            applyPreviewResult(data);
            statusField.textContent = 'LLM 결과 생성이 완료되었습니다. 저장 버튼으로 반영할 수 있습니다.';
            saveButton.disabled = false;
        } finally {
            closeComposeLoading();
        }
    }

    /**
     * @date 2026-04-16
     * @desc 서버 저장 API를 호출해 현재 LLM 결과를 최종 반영합니다.
     */
    async function requestSaveGeneration() {
        const promptField = document.getElementById('composePromptContent');
        const titleField = document.getElementById('composeGeneratedTitle');
        const summaryField = document.getElementById('composeGeneratedSummary');
        const detailField = document.getElementById('composeGeneratedDetail');
        const statusField = document.getElementById('composePreviewStatus');
        const generationContext = getGenerationContext();
        if (!promptField || !titleField || !summaryField || !detailField || !statusField || !generationContext) {
            return;
        }

        if (!titleField.value || !summaryField.value || !detailField.value) {
            statusField.textContent = '저장할 LLM 결과가 없습니다. 먼저 LLM 생성을 실행해주세요.';
            return;
        }

        const payload = {
            targetDate: generationContext.targetDate,
            category: generationContext.category,
            tone: generationContext.tone,
            difficulty: generationContext.difficulty,
            promptContent: promptField.value,
            generatedTitle: titleField.value,
            generatedSummary: summaryField.value,
            generatedDetail: detailField.value
        };

        statusField.textContent = '생성 결과를 저장하는 중입니다...';
        openComposeLoading('생성 결과를 저장하는 중입니다...');
        try {
            const response = await fetch('/admin/generate/save', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getCsrfHeaders()
                },
                body: JSON.stringify(payload)
            });
            const data = await response.json();
            if (!data.success) {
                const codeLabel = data.errorCode ? '[' + data.errorCode + '] ' : '';
                statusField.textContent = codeLabel + (data.message || '저장에 실패했습니다.');
                return;
            }

            if (window.opener && !window.opener.closed) {
                window.opener.location.reload();
            }
            window.close();
        } finally {
            closeComposeLoading();
        }
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창의 버튼 이벤트를 등록합니다.
     */
    function bindComposeActions() {
        const previewButton = document.getElementById('composeGeneratePreviewButton');
        const saveButton = document.getElementById('composeSaveButton');
        const cancelButton = document.getElementById('composeCancelButton');
        const promptField = document.getElementById('composePromptContent');
        const statusField = document.getElementById('composePreviewStatus');
        if (!previewButton || !saveButton || !cancelButton || !promptField || !statusField) {
            return;
        }

        if (promptField.disabled) {
            statusField.textContent = '활성 템플릿이 없어 생성할 수 없습니다.';
            setPreviousSectionEnabled(false);
            saveButton.disabled = true;
            return;
        }

        setPreviousSectionEnabled(false);

        previewButton.addEventListener('click', async () => {
            try {
                await requestPreviewGeneration();
            } catch (error) {
                statusField.textContent = 'LLM 요청 중 오류가 발생했습니다: ' + (error?.message || 'unknown');
            }
        });

        saveButton.addEventListener('click', async () => {
            try {
                await requestSaveGeneration();
            } catch (error) {
                statusField.textContent = '저장 처리 중 오류가 발생했습니다: ' + (error?.message || 'unknown');
            }
        });

        cancelButton.addEventListener('click', () => {
            window.close();
        });
    }

    /**
     * @date 2026-04-16
     * @desc 새창 수동 생성 페이지의 초기화 로직을 실행합니다.
     */
    function initializeComposePage() {
        bindComposeActions();
    }

    initializeComposePage();
})();
