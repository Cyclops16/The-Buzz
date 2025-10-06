// AFTER: spread ...rest so onMouseEnter / onMouseLeave (and any other handler) actually land on the <button>
export default function Button({ className, onClick, children, ...rest }) {
  return (
    <button
      className={className}
      onClick={onClick}
      {...rest}
    >
      {children}
    </button>
  );
}
