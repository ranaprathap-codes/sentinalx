import { render, screen, fireEvent } from '@testing-library/react'
import { Input } from '@/components/ui/Input'

describe('Input', () => {
  it('renders label', () => {
    render(<Input label="Email" />)
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
  })

  it('shows error message', () => {
    render(<Input label="Email" error="Invalid email" />)
    expect(screen.getByText('Invalid email')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
  })

  it('shows helper text', () => {
    render(<Input label="Password" helperText="At least 8 characters" />)
    expect(screen.getByText('At least 8 characters')).toBeInTheDocument()
  })

  it('handles value changes', () => {
    render(<Input label="Name" defaultValue="John" />)
    expect(screen.getByLabelText('Name')).toHaveValue('John')

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Jane' } })
    expect(screen.getByLabelText('Name')).toHaveValue('Jane')
  })

  it('applies disabled state', () => {
    render(<Input label="Disabled" disabled />)
    expect(screen.getByLabelText('Disabled')).toBeDisabled()
  })

  it('forwards ref', () => {
    const ref = jest.fn()
    render(<Input label="Test" ref={ref} />)
    expect(ref).toHaveBeenCalled()
  })
})