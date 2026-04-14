(function () {
    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    }

    function applyRange(startInput, endInput, months) {
        const today = new Date();
        const start = new Date(today);
        start.setMonth(start.getMonth() - months);
        endInput.value = formatDate(today);
        startInput.value = formatDate(start);
    }

    function initSearchToolbar() {
        const startInput = document.getElementById('startDate');
        const endInput = document.getElementById('endDate');
        const dateCompatInput = document.getElementById('dateCompat');
        const form = document.querySelector('.toolbar-form');
        const periodButtons = Array.from(document.querySelectorAll('.period-btn'));

        if (!startInput || !endInput) {
            return;
        }

        if (!startInput.value || !endInput.value) {
            applyRange(startInput, endInput, 3);
        }

        if (dateCompatInput) {
            dateCompatInput.value = endInput.value;
        }

        periodButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                const months = Number(button.dataset.periodMonths || '3');
                applyRange(startInput, endInput, months);
                periodButtons.forEach(function (item) {
                    item.classList.remove('active');
                });
                button.classList.add('active');
                if (dateCompatInput) {
                    dateCompatInput.value = endInput.value;
                }
            });
        });

        endInput.addEventListener('change', function () {
            if (dateCompatInput) {
                dateCompatInput.value = endInput.value;
            }
        });

        if (form) {
            form.addEventListener('submit', function () {
                if (dateCompatInput) {
                    dateCompatInput.value = endInput.value;
                }
            });
        }

        const end = new Date(endInput.value);
        const start = new Date(startInput.value);
        if (!Number.isNaN(end.getTime()) && !Number.isNaN(start.getTime())) {
            const diffMonths = (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth());
            const activeButton = periodButtons.find(function (btn) {
                return Number(btn.dataset.periodMonths) === diffMonths;
            });
            if (activeButton) {
                activeButton.classList.add('active');
            }
        }
    }

    function initManualSlider(root) {
        const track = root.querySelector('[data-track]');
        const viewport = root.querySelector('.manual-viewport');
        const prevButton = root.querySelector('[data-prev]');
        const nextButton = root.querySelector('[data-next]');
        const currentLabel = root.parentElement.querySelector('[data-current-page]');

        if (!track || !viewport || !prevButton || !nextButton) {
            return;
        }

        const totalPages = Number(root.dataset.totalPages || '0');
        if (totalPages <= 1) {
            prevButton.disabled = true;
            nextButton.disabled = true;
            if (currentLabel) {
                currentLabel.textContent = totalPages === 0 ? '0' : '1';
            }
            return;
        }

        let currentPage = 0;
        let wheelLocked = false;

        const update = function () {
            track.style.transform = 'translateX(-' + (currentPage * 100) + '%)';
            prevButton.disabled = currentPage <= 0;
            nextButton.disabled = currentPage >= totalPages - 1;
            if (currentLabel) {
                currentLabel.textContent = String(currentPage + 1);
            }
        };

        const goToPage = function (page) {
            currentPage = Math.max(0, Math.min(totalPages - 1, page));
            update();
        };

        prevButton.addEventListener('click', function () {
            if (currentPage > 0) {
                goToPage(currentPage - 1);
            }
        });

        nextButton.addEventListener('click', function () {
            if (currentPage < totalPages - 1) {
                goToPage(currentPage + 1);
            }
        });

        viewport.addEventListener('wheel', function (event) {
            // Keep vertical wheel scrolling for the page; only use Shift+Wheel for slider navigation.
            if (!event.shiftKey) {
                return;
            }
            event.preventDefault();
            if (wheelLocked) {
                return;
            }
            wheelLocked = true;

            const delta = event.deltaX !== 0 ? event.deltaX : event.deltaY;
            if (delta > 0) {
                goToPage(currentPage + 1);
            } else if (delta < 0) {
                goToPage(currentPage - 1);
            }

            window.setTimeout(function () {
                wheelLocked = false;
            }, 220);
        }, { passive: false });

        update();
    }

    function initHotCarousel(root) {
        const track = root.querySelector('[data-hot-track]');
        const prevButton = root.querySelector('[data-hot-prev]');
        const nextButton = root.querySelector('[data-hot-next]');

        if (!track || !prevButton || !nextButton) {
            return;
        }

        const baseSlides = Array.from(track.children);
        const originalCount = baseSlides.length;

        if (originalCount === 0) {
            prevButton.disabled = true;
            nextButton.disabled = true;
            return;
        }

        const preferredVisible = Number(root.dataset.visible || '3');
        const visibleCount = Math.min(Math.max(preferredVisible, 1), originalCount);
        root.style.setProperty('--hot-visible', String(visibleCount));

        if (originalCount <= visibleCount) {
            prevButton.disabled = true;
            nextButton.disabled = true;
            return;
        }

        const prependClones = baseSlides.slice(-visibleCount).map(function (slide) {
            return slide.cloneNode(true);
        });
        const appendClones = baseSlides.slice(0, visibleCount).map(function (slide) {
            return slide.cloneNode(true);
        });

        prependClones.forEach(function (clone) {
            track.insertBefore(clone, track.firstChild);
        });
        appendClones.forEach(function (clone) {
            track.appendChild(clone);
        });

        let index = visibleCount;
        let timerId = null;
        const intervalMs = 4000;

        const setTranslate = function (animate) {
            track.style.transition = animate ? 'transform 0.42s ease' : 'none';
            const offset = (100 / visibleCount) * index;
            track.style.transform = 'translateX(-' + offset + '%)';
        };

        const moveNext = function () {
            index += 1;
            setTranslate(true);
        };

        const movePrev = function () {
            index -= 1;
            setTranslate(true);
        };

        const startAuto = function () {
            if (timerId !== null) {
                return;
            }
            timerId = window.setInterval(moveNext, intervalMs);
        };

        const stopAuto = function () {
            if (timerId !== null) {
                window.clearInterval(timerId);
                timerId = null;
            }
        };

        track.addEventListener('transitionend', function () {
            const maxIndex = originalCount + visibleCount;

            if (index >= maxIndex) {
                index = visibleCount;
                setTranslate(false);
            } else if (index < visibleCount) {
                index = originalCount + visibleCount - 1;
                setTranslate(false);
            }
        });

        prevButton.addEventListener('click', function () {
            movePrev();
        });

        nextButton.addEventListener('click', function () {
            moveNext();
        });

        root.addEventListener('mouseenter', stopAuto);
        root.addEventListener('mouseleave', startAuto);

        setTranslate(false);
        startAuto();
    }

    document.querySelectorAll('[data-manual-slider]').forEach(initManualSlider);

    const hotRoot = document.querySelector('[data-hot-carousel]');
    if (hotRoot) {
        initHotCarousel(hotRoot);
    }
    initSearchToolbar();

    document.querySelectorAll('.card-link[aria-disabled="true"]').forEach(function (link) {
        link.addEventListener('click', function (event) {
            event.preventDefault();
        });
    });
})();
