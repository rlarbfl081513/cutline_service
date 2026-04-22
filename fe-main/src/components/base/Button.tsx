
interface ButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  disabled?: boolean;
  type?: 'button' | 'submit' | 'reset';
}

export default function Button({ 
  children, 
  onClick, 
  variant = 'primary', 
  size = 'md', 
  className = '',
  disabled = false,
  type = 'button'
}: ButtonProps) {
  const baseClasses = 'whitespace-nowrap cursor-pointer font-medium transition-colors rounded-lg';
  
  const variantClasses = {
    primary: 'bg-[#1FFFA9] text-black hover:bg-[#1FFFA9]/90',
    secondary: 'bg-transparent border border-[#1FFFA9] text-[#1FFFA9] hover:bg-[#1FFFA9] hover:text-black',
    danger: 'bg-[#E43F42] text-white hover:bg-[#E43F42]/90'
  };
  
  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2',
    lg: 'px-6 py-3 text-lg'
  };
  
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]} ${disabled ? 'opacity-50 cursor-not-allowed' : ''} ${className}`}
    >
      {children}
    </button>
  );
}
