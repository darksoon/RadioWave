# Stripe Skills

**Source:** awesome-agent-skills (VoltAgent)

## Stripe API Basics

### Keys unterscheiden
- **Test:** `sk_test_...` (Sandbox)
- **Live:** `sk_live_...` (Produktion)

### Wichtige Endpoints

```
POST /v1/customers
POST /v1/payment_intents
POST /v1/subscriptions
POST /v1/checkout/sessions
GET  /v1/invoices
```

## Checkout Sessions

```javascript
const session = await stripe.checkout.sessions.create({
  payment_method_types: ['card'],
  line_items: [{
    price_data: {
      currency: 'eur',
      product_data: { name: 'Premium Plan' },
      unit_amount: 2900, // €29.00
      recurring: { interval: 'month' }
    },
    quantity: 1
  }],
  mode: 'subscription',
  success_url: 'https://yoursite.com/success',
  cancel_url: 'https://yoursite.com/cancel'
});
```

## Webhooks

```javascript
// Stripe sendet Events
const event = req.body;
// verify with: stripe.webhooks.constructEvent()
```

## Wann aktivieren

- Payments integrieren
- Subscriptions
- Billing
- Stripe API Calls
