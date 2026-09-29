'use client'

import { Fragment, ReactNode } from 'react'
import { ChevronDown, Check } from 'lucide-react'
import { useState, useRef, useEffect } from 'react'
import { cn } from '@/lib/utils'
import { Button } from './Button'

interface DropdownItem {
  label: string
  value: string
  icon?: ReactNode
  disabled?: boolean
  danger?: boolean
}

interface DropdownMenuProps {
  trigger: ReactNode
  items: DropdownItem[]
  onSelect: (value: string) => void
  align?: 'left' | 'right'
  className?: string
}

export function DropdownMenu({
  trigger,
  items,
  onSelect,
  align = 'right',
  className,
}: DropdownMenuProps) {
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [isOpen])

  return (
    <div className="relative inline-block" ref={dropdownRef}>
      <Button
        variant="outline"
        onClick={() => setIsOpen(!isOpen)}
        className="gap-1"
        aria-haspopup="true"
        aria-expanded={isOpen}
      >
        {trigger}
        <ChevronDown className={cn('h-4 w-4 transition-transform', isOpen && 'rotate-180')} />
      </Button>

      {isOpen && (
        <div
          className={cn(
            'absolute z-50 mt-1 min-w-[160px] bg-sentinel-card border border-sentinel-border rounded-lg shadow-lg animate-fade-in',
            align === 'right' ? 'right-0' : 'left-0',
            className
          )}
          role="menu"
        >
          {items.map((item) => (
            <button
              key={item.value}
              onClick={() => {
                onSelect(item.value)
                setIsOpen(false)
              }}
              disabled={item.disabled}
              role="menuitem"
              className={cn(
                'w-full px-4 py-2 text-left text-sm flex items-center gap-2 transition-colors',
                'hover:bg-sentinel-border focus:outline-none focus:bg-sentinel-border',
                item.disabled ? 'opacity-50 cursor-not-allowed' : 'text-white',
                item.danger ? 'text-red-400 hover:text-red-300' : ''
              )}
            >
              {item.icon && <span className="h-4 w-4">{item.icon}</span>}
              <span>{item.label}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

interface SelectProps {
  value: string
  onChange: (value: string) => void
  options: { label: string; value: string }[]
  placeholder?: string
  className?: string
  disabled?: boolean
}

export function Select({
  value,
  onChange,
  options,
  placeholder,
  className,
  disabled,
}: SelectProps) {
  const [isOpen, setIsOpen] = useState(false)
  const selectRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (selectRef.current && !selectRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [isOpen])

  const selectedOption = options.find((opt) => opt.value === value)

  return (
    <div className="relative" ref={selectRef}>
      <Button
        variant="outline"
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        className={cn('w-full justify-between', className)}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
      >
        <span className={cn('truncate', !selectedOption && 'text-slate-500')}>
          {selectedOption?.label || placeholder || 'Select...'}
        </span>
        <ChevronDown className={cn('h-4 w-4 flex-shrink-0 ml-2 transition-transform', isOpen && 'rotate-180')} />
      </Button>

      {isOpen && (
        <div className="absolute z-50 mt-1 w-full bg-sentinel-card border border-sentinel-border rounded-lg shadow-lg animate-fade-in max-h-60 overflow-auto">
          {options.map((option) => (
            <button
              key={option.value}
              onClick={() => {
                onChange(option.value)
                setIsOpen(false)
              }}
              role="option"
              aria-selected={option.value === value}
              className={cn(
                'w-full px-4 py-2 text-left text-sm flex items-center gap-2 transition-colors',
                'hover:bg-sentinel-border focus:outline-none focus:bg-sentinel-border',
                option.value === value ? 'bg-primary-500/20 text-primary-400' : 'text-white hover:text-white'
              )}
            >
              <span>{option.label}</span>
              {option.value === value && <Check className="h-4 w-4 ml-auto text-primary-500" />}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}