(function () {
    /**
     * @date 2026-04-24
     * @desc CSRF 메타 태그에서 헤더 이름과 토큰 값을 읽어옵니다.
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
     * @date 2026-04-24
     * @desc 현재 수동 생성 대상 조건을 dataset에서 읽어옵니다.
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
     * @date 2026-04-24
     * @desc 작업 진행 중 로딩 오버레이를 표시합니다.
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
     * @date 2026-04-24
     * @desc 로딩 오버레이를 숨깁니다.
     */
    function closeComposeLoading() {
        const loadingOverlay = document.getElementById('composeLoadingOverlay');
        if (!loadingOverlay) {
            return;
        }
        loadingOverlay.hidden = true;
    }

    /**
     * @date 2026-04-24
     * @desc 이미지 URL 입력값을 표시 가능한 형태로 정규화합니다.
     */
    function normalizePreviewImageUrl(imageUrl) {
        if (!imageUrl) {
            return '';
        }
        const normalizedImageUrl = String(imageUrl).trim();
        if (!normalizedImageUrl || normalizedImageUrl === 'null' || normalizedImageUrl === 'undefined') {
            return '';
        }
        if (normalizedImageUrl.startsWith('http://') || normalizedImageUrl.startsWith('https://') || normalizedImageUrl.startsWith('/')) {
            return normalizedImageUrl;
        }
        return '/' + normalizedImageUrl;
    }

    /**
     * @date 2026-04-24
     * @desc 미리보기 이미지 영역을 URL 유무에 맞춰 갱신합니다.
     */
    function applyComposePreviewImage(imageElementId, emptyElementId, imageUrl) {
        const imageElement = document.getElementById(imageElementId);
        const emptyElement = document.getElementById(emptyElementId);
        if (!imageElement || !emptyElement) {
            return;
        }

        const normalizedImageUrl = normalizePreviewImageUrl(imageUrl);
        if (!normalizedImageUrl) {
            imageElement.src = '';
            imageElement.hidden = true;
            emptyElement.hidden = false;
            return;
        }

        imageElement.onerror = () => {
            imageElement.hidden = true;
            emptyElement.hidden = false;
        };
        imageElement.src = normalizedImageUrl;
        imageElement.hidden = false;
        emptyElement.hidden = true;
    }

    /**
     * @date 2026-04-24
     * @desc 이전 결과 영역의 활성/비활성 UI를 갱신합니다.
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

        if (!enabled) {
            applyComposePreviewImage('composePreviousImage', 'composePreviousImageEmpty', '');
        }
    }

    /**
     * @date 2026-04-24
     * @desc LLM 미리보기 응답 데이터를 화면에 반영합니다.
     */
    function applyPreviewResult(previewResponse) {
        const titleField = document.getElementById('composeGeneratedTitle');
        const summaryField = document.getElementById('composeGeneratedSummary');
        const detailField = document.getElementById('composeGeneratedDetail');
        const previousTitleField = document.getElementById('composePreviousTitle');
        const previousSummaryField = document.getElementById('composePreviousSummary');
        const previousDetailField = document.getElementById('composePreviousDetail');
        const dailyTrendIdField = document.getElementById('composeDailyTrendId');

        if (!titleField || !summaryField || !detailField || !previousTitleField || !previousSummaryField || !previousDetailField) {
            return;
        }

        titleField.value = previewResponse.generatedTitle || '';
        summaryField.value = previewResponse.generatedSummary || '';
        detailField.value = previewResponse.generatedDetail || '';
        if (dailyTrendIdField) {
            dailyTrendIdField.value = previewResponse.dailyTrendId || '';
        }
        applyComposePreviewImage('composeGeneratedImage', 'composeGeneratedImageEmpty', previewResponse.generatedImageUrl || '');

        if (previewResponse.hasPreviousResult) {
            previousTitleField.value = previewResponse.previousTitle || '';
            previousSummaryField.value = previewResponse.previousSummary || '';
            previousDetailField.value = previewResponse.previousDetail || '';
            applyComposePreviewImage('composePreviousImage', 'composePreviousImageEmpty', previewResponse.previousImageUrl || '');
            setPreviousSectionEnabled(true);
            return;
        }

        previousTitleField.value = '';
        previousSummaryField.value = '';
        previousDetailField.value = '';
        setPreviousSectionEnabled(false);
    }

    /**
     * @date 2026-04-24
     * @desc 미리보기 API를 호출해 LLM 결과를 생성합니다.
     */
    async function requestPreviewGeneration() {
        const promptField = document.getElementById('composePromptContent');
        const imagePromptTemplateField = document.getElementById('composeImagePromptTemplate');
        const statusField = document.getElementById('composePreviewStatus');
        const saveButton = document.getElementById('composeSaveButton');
        const generationContext = getGenerationContext();
        if (!promptField || !imagePromptTemplateField || !statusField || !saveButton || !generationContext) {
            return;
        }

        const payload = {
            targetDate: generationContext.targetDate,
            category: generationContext.category,
            tone: generationContext.tone,
            difficulty: generationContext.difficulty,
            promptContent: promptField.value,
            imagePromptTemplate: imagePromptTemplateField.value
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
     * @date 2026-04-24
     * @desc 저장 API를 호출해 현재 LLM 결과를 최종 반영합니다.
     */
    async function requestSaveGeneration() {
        const promptField = document.getElementById('composePromptContent');
        const titleField = document.getElementById('composeGeneratedTitle');
        const summaryField = document.getElementById('composeGeneratedSummary');
        const detailField = document.getElementById('composeGeneratedDetail');
        const generatedImageField = document.getElementById('composeGeneratedImage');
        const statusField = document.getElementById('composePreviewStatus');
        const dailyTrendIdField = document.getElementById('composeDailyTrendId');
        const generationContext = getGenerationContext();
        if (!promptField || !titleField || !summaryField || !detailField || !statusField || !generationContext) {
            return;
        }

        if (!titleField.value || !summaryField.value || !detailField.value) {
            statusField.textContent = '저장할 LLM 결과가 없습니다. 먼저 LLM 생성을 실행해주세요.';
            return;
        }

        const generatedImageUrl = (!generatedImageField || generatedImageField.hidden)
            ? ''
            : normalizePreviewImageUrl(generatedImageField.getAttribute('src') || '');

        const payload = {
            targetDate: generationContext.targetDate,
            category: generationContext.category,
            tone: generationContext.tone,
            difficulty: generationContext.difficulty,
            promptContent: promptField.value,
            generatedTitle: titleField.value,
            generatedSummary: summaryField.value,
            generatedDetail: detailField.value,
            generatedImageUrl,
            dailyTrendId: dailyTrendIdField && dailyTrendIdField.value
                ? Number(dailyTrendIdField.value)
                : null
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
     * @date 2026-04-24
     * @desc LLM 결과 이미지 미리보기를 재생성합니다.
     */
    async function requestRefreshGeneratedImage() {
        const titleField = document.getElementById('composeGeneratedTitle');
        const summaryField = document.getElementById('composeGeneratedSummary');
        const detailField = document.getElementById('composeGeneratedDetail');
        const imagePromptTemplateField = document.getElementById('composeImagePromptTemplate');
        const statusField = document.getElementById('composePreviewStatus');
        const generationContext = getGenerationContext();
        if (!titleField || !summaryField || !detailField || !imagePromptTemplateField || !statusField || !generationContext) {
            return;
        }

        if (!titleField.value) {
            window.alert('이미지 새로고침 전에 LLM 생성을 먼저 실행해주세요.');
            return;
        }

        const payload = {
            targetDate: generationContext.targetDate,
            category: generationContext.category,
            generatedTitle: titleField.value,
            generatedSummary: summaryField.value,
            generatedDetail: detailField.value,
            imagePromptTemplate: imagePromptTemplateField.value
        };

        statusField.textContent = '이미지를 새로고침하는 중입니다...';
        openComposeLoading('이미지를 새로고침하는 중입니다...');
        try {
            const response = await fetch('/admin/generate/preview/image-refresh', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...getCsrfHeaders()
                },
                body: JSON.stringify(payload)
            });
            const data = await response.json();
            if (!data.success) {
                statusField.textContent = data.message || '이미지 새로고침에 실패했습니다.';
                window.alert(statusField.textContent);
                return;
            }

            applyComposePreviewImage('composeGeneratedImage', 'composeGeneratedImageEmpty', data.imageUrl || '');
            statusField.textContent = data.message || '이미지를 새로고침했습니다.';
            window.alert(statusField.textContent);
        } finally {
            closeComposeLoading();
        }
    }

    /**
     * @date 2026-04-24
     * @desc 모달 버튼 이벤트를 바인딩합니다.
     */
    function bindComposeActions() {
        const previewButton = document.getElementById('composeGeneratePreviewButton');
        const saveButton = document.getElementById('composeSaveButton');
        const cancelButton = document.getElementById('composeCancelButton');
        const imageRefreshButton = document.getElementById('composeGeneratedImageRefreshButton');
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

        if (imageRefreshButton) {
            imageRefreshButton.addEventListener('click', async () => {
                try {
                    await requestRefreshGeneratedImage();
                } catch (error) {
                    const fallbackMessage = '이미지 새로고침 중 오류가 발생했습니다: ' + (error?.message || 'unknown');
                    statusField.textContent = fallbackMessage;
                    window.alert(fallbackMessage);
                }
            });
        }
    }

    /**
     * @date 2026-04-24
     * @desc 수동 생성 확인 페이지 초기화 로직을 실행합니다.
     */
    function initializeComposePage() {
        applyComposePreviewImage('composeGeneratedImage', 'composeGeneratedImageEmpty', '');
        applyComposePreviewImage('composePreviousImage', 'composePreviousImageEmpty', '');
        bindComposeActions();
    }

    initializeComposePage();
})();
