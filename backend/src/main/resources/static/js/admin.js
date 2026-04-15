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
        });
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 페이지 초기 스크립트를 실행합니다.
     */
    function initializeAdminPage() {
        bindTemplateLoadButtons();
        bindResetButton();
    }

    initializeAdminPage();
})();
