(function () {
    /**
     * @date 2026-04-13
     * @desc JSON 요청 공통 처리 및 오류 메시지 변환을 수행합니다.
     */
    async function requestJson(url, options) {
        const response = await fetch(url, options);
        if (!response.ok) {
            let message = '요청 처리 중 오류가 발생했습니다.';
            try {
                const errorBody = await response.json();
                if (errorBody && errorBody.message) {
                    message = errorBody.message;
                }
            } catch (ignored) {
                message = '요청 처리 중 오류가 발생했습니다.';
            }
            throw new Error(message);
        }
        return response.json();
    }

    /**
     * @date 2026-04-13
     * @desc 상세 페이지 DOM에 집계 데이터와 버튼 상태를 반영합니다.
     */
    function renderEngagement(root, detailState) {
        const viewCountNode = root.querySelector('[data-view-count]');
        const likeCountNode = root.querySelector('[data-like-count]');
        const bookmarkCountNode = root.querySelector('[data-bookmark-count]');
        const commentCountNode = root.querySelector('[data-comment-count]');
        const likeButton = root.querySelector('[data-like-button]');
        const bookmarkButton = root.querySelector('[data-bookmark-button]');

        if (viewCountNode) {
            viewCountNode.textContent = String(detailState.viewCount || 0);
        }
        if (likeCountNode) {
            likeCountNode.textContent = String(detailState.likeCount || 0);
        }
        if (bookmarkCountNode) {
            bookmarkCountNode.textContent = String(detailState.bookmarkCount || 0);
        }
        if (commentCountNode && Array.isArray(detailState.comments)) {
            commentCountNode.textContent = String(detailState.comments.length);
        }
        if (likeButton) {
            likeButton.classList.toggle('active', !!detailState.liked);
        }
        if (bookmarkButton) {
            bookmarkButton.classList.toggle('active', !!detailState.bookmarked);
        }
    }

    /**
     * @date 2026-04-13
     * @desc 댓글 목록을 최신 상태로 재렌더링합니다.
     */
    function renderComments(root, comments) {
        const listNode = root.querySelector('[data-comment-list]');
        if (!listNode) {
            return;
        }

        if (!Array.isArray(comments) || comments.length === 0) {
            listNode.innerHTML = '<li class="comment-empty">등록된 댓글이 없습니다.</li>';
            return;
        }

        listNode.innerHTML = comments.map(function (comment) {
            const createdAt = comment.createdAt ? String(comment.createdAt).replace('T', ' ').slice(0, 16) : '';
            const deleteButton = comment.mine
                ? '<button type="button" class="delete-button" data-comment-delete>삭제</button>'
                : '';
            return [
                '<li class="comment-item" data-comment-id="' + comment.id + '">',
                '<div class="comment-meta">',
                '<strong>' + escapeHtml(comment.authorName || '') + '</strong>',
                '<span>' + escapeHtml(createdAt) + '</span>',
                '</div>',
                '<p>' + escapeHtml(comment.content || '') + '</p>',
                deleteButton,
                '</li>'
            ].join('');
        }).join('');
    }

    /**
     * @date 2026-04-13
     * @desc HTML 문자열 이스케이프를 수행합니다.
     */
    function escapeHtml(value) {
        return value
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    /**
     * @date 2026-04-13
     * @desc CSRF 헤더 정보를 DOM 메타태그에서 조회합니다.
     */
    function buildCsrfHeaders() {
        const tokenNode = document.querySelector('meta[name="_csrf"]');
        const headerNode = document.querySelector('meta[name="_csrf_header"]');
        if (!tokenNode || !headerNode) {
            return {};
        }
        return {
            [headerNode.content]: tokenNode.content
        };
    }

    /**
     * @date 2026-04-13
     * @desc 상세 페이지 상호작용 이벤트를 초기화합니다.
     */
    function initDetailPage() {
        const root = document.querySelector('.detail-page');
        if (!root) {
            return;
        }

        const apiPath = root.dataset.apiPath;
        const likeButton = root.querySelector('[data-like-button]');
        const bookmarkButton = root.querySelector('[data-bookmark-button]');
        const commentForm = root.querySelector('[data-comment-form]');
        const commentInput = root.querySelector('[data-comment-input]');
        const csrfHeaders = buildCsrfHeaders();

        if (!apiPath) {
            return;
        }

        const renderFromState = function (state) {
            renderEngagement(root, state);
            renderComments(root, state.comments || []);
        };

        if (likeButton) {
            likeButton.addEventListener('click', async function () {
                try {
                    const result = await requestJson(apiPath + '/likes/toggle', {
                        method: 'POST',
                        headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders)
                    });
                    likeButton.classList.toggle('active', !!result.active);
                    const likeCountNode = root.querySelector('[data-like-count]');
                    if (likeCountNode) {
                        likeCountNode.textContent = String(result.count || 0);
                    }
                } catch (error) {
                    window.alert(error.message);
                }
            });
        }

        if (bookmarkButton) {
            bookmarkButton.addEventListener('click', async function () {
                try {
                    const result = await requestJson(apiPath + '/bookmarks/toggle', {
                        method: 'POST',
                        headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders)
                    });
                    bookmarkButton.classList.toggle('active', !!result.active);
                    const bookmarkCountNode = root.querySelector('[data-bookmark-count]');
                    if (bookmarkCountNode) {
                        bookmarkCountNode.textContent = String(result.count || 0);
                    }
                } catch (error) {
                    window.alert(error.message);
                }
            });
        }

        if (commentForm && commentInput) {
            commentForm.addEventListener('submit', async function (event) {
                event.preventDefault();
                const content = commentInput.value.trim();
                if (!content) {
                    window.alert('댓글 내용을 입력해 주세요.');
                    return;
                }

                try {
                    const state = await requestJson(apiPath + '/comments', {
                        method: 'POST',
                        headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders),
                        body: JSON.stringify({ content: content })
                    });
                    renderFromState(state);
                    commentInput.value = '';
                } catch (error) {
                    window.alert(error.message);
                }
            });
        }

        const commentList = root.querySelector('[data-comment-list]');
        if (commentList) {
            commentList.addEventListener('click', async function (event) {
                const target = event.target;
                if (!(target instanceof HTMLElement) || !target.matches('[data-comment-delete]')) {
                    return;
                }
                const item = target.closest('[data-comment-id]');
                if (!item) {
                    return;
                }
                const commentId = item.getAttribute('data-comment-id');
                if (!commentId) {
                    return;
                }

                try {
                    const state = await requestJson(apiPath + '/comments/' + encodeURIComponent(commentId), {
                        method: 'DELETE',
                        headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders)
                    });
                    renderFromState(state);
                } catch (error) {
                    window.alert(error.message);
                }
            });
        }
    }

    initDetailPage();
})();
