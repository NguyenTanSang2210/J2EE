// Update cart count in header
function updateCartCount() {
    fetch('/cart/count')
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            return { count: 0 };
        })
        .then(data => {
            const cartBadge = document.getElementById('cartCount');
            if (cartBadge) {
                cartBadge.textContent = data.count || 0;
            }
        })
        .catch(error => {
            console.error('Error loading cart count:', error);
        });
}

$(document).ready(function () {
    // Load cart count on page load
    updateCartCount();
    
    // Update quantity when changed
    $('.quantity').change(function () {
        let quantity = $(this).val();
        let id = $(this).attr('data-id');
        $.ajax({
            url: '/cart/updateCart/' + id + '/' + quantity,
            type: 'GET',
            success: function () {
                location.reload();
            }
        });
    });
});