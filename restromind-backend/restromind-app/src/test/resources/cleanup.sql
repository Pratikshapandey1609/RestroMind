-- Clean up test data before each integration test run
DELETE FROM restromind_menu.dishes WHERE restaurant_id IN (
    SELECT id FROM restromind_restaurant.restaurants WHERE owner_id IN (
        SELECT id FROM restromind_auth.users WHERE email IN ('admin@test.com','user@test.com')
    )
);
DELETE FROM restromind_menu.categories WHERE restaurant_id IN (
    SELECT id FROM restromind_restaurant.restaurants WHERE owner_id IN (
        SELECT id FROM restromind_auth.users WHERE email IN ('admin@test.com','user@test.com')
    )
);
DELETE FROM restromind_restaurant.operating_hours WHERE restaurant_id IN (
    SELECT id FROM restromind_restaurant.restaurants WHERE owner_id IN (
        SELECT id FROM restromind_auth.users WHERE email IN ('admin@test.com','user@test.com')
    )
);
DELETE FROM restromind_restaurant.restaurants WHERE owner_id IN (
    SELECT id FROM restromind_auth.users WHERE email IN ('admin@test.com','user@test.com')
);
DELETE FROM restromind_auth.refresh_tokens WHERE user_id IN (
    SELECT id FROM restromind_auth.users WHERE email IN ('admin@test.com','user@test.com')
);
DELETE FROM restromind_auth.users WHERE email IN ('admin@test.com','user@test.com');
