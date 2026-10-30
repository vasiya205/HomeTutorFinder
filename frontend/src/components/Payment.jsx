import { useState } from 'react'
import { CheckCircle2, CreditCard, Lock, ShieldCheck } from 'lucide-react'
import Modal from './Modal'
import { Spinner } from './Feedback'
import { api } from '../lib/api'
import { formatMoney, formatDateTime } from '../lib/format'
import { useToast } from '../context/ToastContext'

function loadRazorpayScript() {
  if (window.Razorpay) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.onload = resolve
    script.onerror = () => reject(new Error('Could not load Razorpay Checkout. Check your internet connection.'))
    document.body.appendChild(script)
  })
}

/**
 * Starts a payment for an accepted booking.
 * RAZORPAY mode opens the real Razorpay test checkout; DEMO mode opens the built-in checkout.
 * Either way the backend verifies the payment before the booking is marked PAID.
 */
export function usePayment(onPaid) {
  const toast = useToast()
  const [payingId, setPayingId] = useState(null)
  const [demo, setDemo] = useState(null)

  const finish = (result) => {
    onPaid?.(result.booking)
    toast.success(result.message)
  }

  const pay = async (booking) => {
    setPayingId(booking.id)
    try {
      const order = await api.createPaymentOrder(booking.id)
      if (order.mode !== 'RAZORPAY') {
        setDemo({ booking, order })
        return
      }
      await loadRazorpayScript()
      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: 'Home Tutor Finder',
        description: order.description,
        order_id: order.orderId,
        prefill: { name: order.customerName, email: order.customerEmail },
        notes: { bookingId: String(booking.id) },
        theme: { color: '#4f46e5' },
        handler: async (response) => {
          try {
            finish(await api.verifyPayment(booking.id, {
              orderId: response.razorpay_order_id,
              paymentId: response.razorpay_payment_id,
              signature: response.razorpay_signature,
            }))
          } catch (error) {
            toast.error(error.message)
          } finally {
            setPayingId(null)
          }
        },
        modal: { ondismiss: () => setPayingId(null) },
      })
      checkout.on('payment.failed', (response) => {
        toast.error(response?.error?.description || 'Payment failed. You can try again.')
        setPayingId(null)
      })
      checkout.open()
    } catch (error) {
      toast.error(error.message)
      setPayingId(null)
    }
  }

  const modal = demo && (
    <DemoCheckout
      booking={demo.booking}
      order={demo.order}
      onClose={() => {
        setDemo(null)
        setPayingId(null)
      }}
      onPaid={finish}
    />
  )

  return { pay, payingId, modal }
}

function DemoCheckout({ booking, order, onClose, onPaid }) {
  const [card, setCard] = useState({ number: '4111 1111 1111 1111', expiry: '12/30', cvv: '123', name: order.customerName || '' })
  const [stage, setStage] = useState('form')
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)

  const submit = async (event) => {
    event.preventDefault()
    if (card.number.replace(/\s/g, '').length < 12 || card.cvv.length < 3) {
      setError('Enter the card details (any test values work in demo mode).')
      return
    }
    setError('')
    setStage('processing')
    try {
      await new Promise((resolve) => setTimeout(resolve, 1200))
      const paid = await api.verifyPayment(booking.id, { orderId: order.orderId })
      setResult(paid)
      setStage('done')
      onPaid(paid)
    } catch (paymentError) {
      setError(paymentError.message)
      setStage('form')
    }
  }

  return (
    <Modal title="Secure checkout" onClose={stage === 'processing' ? undefined : onClose} width={460}>
      <div className="checkout-summary">
        <div>
          <p className="muted small">{order.description}</p>
          <p className="muted small">{formatDateTime(booking.startDateTime)}</p>
        </div>
        <p className="checkout-amount">{formatMoney(order.totalAmount)}</p>
      </div>

      {stage === 'done' ? (
        <div className="checkout-success">
          <CheckCircle2 size={56} />
          <h4>Payment successful</h4>
          <p>Payment ID <code>{result?.paymentId}</code></p>
          <p className="muted small">A receipt has been emailed to you and the tutor has been notified.</p>
          <button type="button" className="btn btn-primary btn-block" onClick={onClose}>Done</button>
        </div>
      ) : (
        <form onSubmit={submit} className="form-grid">
          <div className="demo-banner">
            <ShieldCheck size={16} />
            <span><strong>Demo checkout</strong> - Razorpay keys are not configured, so this simulates the payment. No real money is charged.</span>
          </div>
          <label className="field">
            <span>Card number</span>
            <div className="input-icon">
              <CreditCard size={18} />
              <input value={card.number} inputMode="numeric" maxLength={19}
                onChange={(e) => setCard({ ...card, number: e.target.value })} />
            </div>
          </label>
          <div className="field-row">
            <label className="field">
              <span>Expiry</span>
              <input value={card.expiry} maxLength={5} onChange={(e) => setCard({ ...card, expiry: e.target.value })} />
            </label>
            <label className="field">
              <span>CVV</span>
              <input value={card.cvv} maxLength={4} inputMode="numeric" type="password"
                onChange={(e) => setCard({ ...card, cvv: e.target.value })} />
            </label>
          </div>
          <label className="field">
            <span>Name on card</span>
            <input value={card.name} onChange={(e) => setCard({ ...card, name: e.target.value })} />
          </label>
          {error && <p className="field-error">{error}</p>}
          <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={stage === 'processing'}>
            {stage === 'processing' ? <Spinner label="Processing payment..." /> : <><Lock size={16} /> Pay {formatMoney(order.totalAmount)}</>}
          </button>
          <p className="muted small center">Order {order.orderId}</p>
        </form>
      )}
    </Modal>
  )
}
