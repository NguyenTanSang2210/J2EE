// Wishlist functionality
// Get CSRF token from meta tags
function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || '';
}

// Toggle wishlist status - GLOBAL function for onclick
window.toggleWishlist = function(btn) {
    const bookId = btn.getAttribute('data-book-id');
    const heartIcon = btn.querySelector('.heart-icon') || btn.querySelector('i.bi-heart');
    
    if (!heartIcon) {
        console.error('Heart icon not found');
        return;
    }
    
    // Check if heart is filled (either ♥ text or has 'bi-heart-fill' class)
    const isFilled = heartIcon.textContent.trim() === '♥' || heartIcon.classList.contains('bi-heart-fill');
        
        const url = isFilled ? `/wishlist/remove/${bookId}` : `/wishlist/add/${bookId}`;
        const method = isFilled ? 'DELETE' : 'POST';
        
        const headers = {
            'Content-Type': 'application/json'
        };
        
        const csrfToken = getCsrfToken();
        const csrfHeader = getCsrfHeader();
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        
        // Disable button during request
        btn.disabled = true;
        
        fetch(url, {
            method: method,
            headers: headers
        })
        .then(response => {
            if (response.status === 401) {
                // Not authenticated
                window.location.href = '/login';
                throw new Error('Unauthorized');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                // Toggle heart appearance
                if (heartIcon.tagName === 'I') {
                    // For <i> tag, toggle bi-heart and bi-heart-fill classes
                    if (isFilled) {
                        heartIcon.classList.remove('bi-heart-fill');
                        heartIcon.classList.add('bi-heart');
                        heartIcon.style.color = '#6c757d';
                    } else {
                        heartIcon.classList.remove('bi-heart');
                        heartIcon.classList.add('bi-heart-fill');
                        heartIcon.style.color = '#dc3545';
                    }
                } else {
                    // For <span> tag, use text content
                    heartIcon.textContent = isFilled ? '♡' : '♥';
                    heartIcon.style.color = isFilled ? '#6c757d' : '#dc3545';
                }
                btn.classList.toggle('active', !isFilled);
                
                // Update wishlist count in navbar
                updateWishlistCount(data.count);
                
                // Show toast notification
                showToast(data.message, 'success');
            } else {
                showToast(data.message || 'Có lỗi xảy ra', 'error');
            }
        })
        .catch(error => {
            if (error.message !== 'Unauthorized') {
                console.error('Error:', error);
                showToast('Có lỗi xảy ra khi thao tác', 'error');
            }
        })
        .finally(() => {
            btn.disabled = false;
        });
    };

    // Update wishlist count badge
    function updateWishlistCount(count) {
        const countBadge = document.getElementById('wishlistCount');
        if (countBadge) {
            countBadge.textContent = count;
            // Add animation
            countBadge.style.animation = 'none';
            setTimeout(() => {
                countBadge.style.animation = 'pulse 0.3s ease';
            }, 10);
        }
    }

    // Show toast notification
    function showToast(message, type = 'success') {
        // Remove existing toasts
        const existingToasts = document.querySelectorAll('.toast-notification');
        existingToasts.forEach(toast => toast.remove());
        
        const toast = document.createElement('div');
        toast.className = `toast-notification ${type}`;
        toast.innerHTML = `
            <i class="bi bi-${type === 'success' ? 'check-circle' : 'x-circle'} me-2"></i>
            ${message}
        `;
        toast.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            background: ${type === 'success' ? '#28a745' : '#dc3545'};
            color: white;
            padding: 15px 25px;
            border-radius: 8px;
            z-index: 9999;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            font-weight: 500;
            display: flex;
            align-items: center;
            animation: slideInRight 0.3s ease-out;
            max-width: 350px;
        `;
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease-in';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    // Check wishlist status for all books on page load
    function checkWishlistStatus() {
        const wishlistButtons = document.querySelectorAll('.wishlist-btn, .product-wishlist-btn');
        
        if (wishlistButtons.length === 0) return;
        
        wishlistButtons.forEach(btn => {
            const bookId = btn.getAttribute('data-book-id');
            const heartIcon = btn.querySelector('.heart-icon') || btn.querySelector('i.bi-heart, i.bi-heart-fill');
            
            if (!bookId || !heartIcon) return;
            
            fetch(`/wishlist/check/${bookId}`)
                .then(response => {
                    // User not authenticated - skip
                    if (!response.ok) {
                        if (response.status === 401 || response.status === 302 || response.status === 403) {
                            // Set default appearance for unauthenticated users
                            if (heartIcon.tagName === 'I') {
                                heartIcon.classList.remove('bi-heart-fill');
                                heartIcon.classList.add('bi-heart');
                                heartIcon.style.color = '#6c757d';
                            } else {
                                heartIcon.textContent = '♡';
                                heartIcon.style.color = '#6c757d';
                            }
                            btn.classList.remove('active');
                            return null;
                        }
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    if (data) {
                        if (data.inWishlist) {
                            if (heartIcon.tagName === 'I') {
                                heartIcon.classList.remove('bi-heart');
                                heartIcon.classList.add('bi-heart-fill');
                                heartIcon.style.color = '#dc3545';
                            } else {
                                heartIcon.textContent = '♥';
                                heartIcon.style.color = '#dc3545';
                            }
                            btn.classList.add('active');
                        } else {
                            if (heartIcon.tagName === 'I') {
                                heartIcon.classList.remove('bi-heart-fill');
                                heartIcon.classList.add('bi-heart');
                                heartIcon.style.color = '#6c757d';
                            } else {
                                heartIcon.textContent = '♡';
                                heartIcon.style.color = '#6c757d';
                            }
                            btn.classList.remove('active');
                        }
                    }
                })
                .catch(error => {
                    // Silently handle errors - set default appearance
                    if (heartIcon.tagName === 'I') {
                        heartIcon.classList.remove('bi-heart-fill');
                        heartIcon.classList.add('bi-heart');
                        heartIcon.style.color = '#6c757d';
                    } else {
                        heartIcon.textContent = '♡';
                        heartIcon.style.color = '#6c757d';
                    }
                    btn.classList.remove('active');
                });
        });
    }

    // Load wishlist count on page load
    function loadWishlistCount() {
        fetch('/wishlist/count')
            .then(response => {
                // Check if user is authenticated
                if (!response.ok) {
                    // User not logged in or error - set count to 0
                    if (response.status === 401 || response.status === 302 || response.status === 403) {
                        updateWishlistCount(0);
                        return null;
                    }
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                if (data) {
                    updateWishlistCount(data.count);
                }
            })
            .catch(error => {
                // Silently handle errors for unauthenticated users
                // Set count to 0 instead of showing error
                updateWishlistCount(0);
            });
    }

    // Initialize on DOM content loaded
    document.addEventListener('DOMContentLoaded', function() {
        // Add CSS animations
        if (!document.getElementById('wishlist-animations')) {
            const style = document.createElement('style');
            style.id = 'wishlist-animations';
            style.textContent = `
                @keyframes slideInRight {
                    from {
                        transform: translateX(400px);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
                @keyframes slideOutRight {
                    from {
                        transform: translateX(0);
                        opacity: 1;
                    }
                    to {
                        transform: translateX(400px);
                        opacity: 0;
                    }
                }
                @keyframes pulse {
                    0%, 100% {
                        transform: scale(1);
                    }
                    50% {
                        transform: scale(1.2);
                    }
                }
                .wishlist-btn {
                    transition: transform 0.2s ease;
                }
                .wishlist-btn:hover {
                    transform: scale(1.1);
                }
                .wishlist-btn.active .heart-icon {
                    animation: pulse 0.3s ease;
                }
            `;
            document.head.appendChild(style);
        }
        
        // Check wishlist status for all books
        checkWishlistStatus();
        
        // Load wishlist count
        loadWishlistCount();
    });
